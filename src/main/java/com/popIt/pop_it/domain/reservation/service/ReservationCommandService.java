package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.contract.service.ContractService;
import com.popIt.pop_it.domain.payment.service.PaymentService;
import com.popIt.pop_it.domain.reservation.converter.ReservationConverter;
import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.CheckoutImage;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.CheckoutImageRepository;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.exception.SpaceErrorCode;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.exception.code.UserErrorCode;
import com.popIt.pop_it.domain.user.repository.UserRepository;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommandService {

    private static final BigDecimal INSURANCE_RATE = BigDecimal.valueOf(0.05);
    private static final BigDecimal PLATFORM_FEE_RATE = BigDecimal.valueOf(0.10);
    private static final int MAX_RESERVATION_DAYS = 90;

    private final ReservationRepository reservationRepository;
    private final CheckoutImageRepository checkoutImageRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final ContractService contractService;

    //예약 요청
    public ReservationResDTO.ReservationCreateRes createReservation(Long userId, ReservationReqDTO.ReservationCreateReq request) {
        Space space;
        try {
            space = spaceRepository.findByIdForUpdate(request.spaceId())
                    .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));
        } catch (PessimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ALREADY_TAKEN);
        }
        User guest = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        if (space.getHostId().equals(userId)) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_SELF_BOOKING_NOT_ALLOWED);
        }

        validateDateRange(space, request.startDate(), request.endDate());

        boolean overlapping = reservationRepository.existsOverlappingReservation(
                space.getId(), request.startDate(), request.endDate(),
                List.of(ReservationStatus.CANCELLED)
        );
        if (overlapping) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ALREADY_TAKEN);
        }

        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        Long rentalFee = (long) space.getPricePerDay() * days;
        Long insuranceFee = BigDecimal.valueOf(rentalFee)
                .multiply(INSURANCE_RATE)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
        Long platformFee = BigDecimal.valueOf(rentalFee)
                .multiply(PLATFORM_FEE_RATE)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
        Long deposit = space.getDeposit();
        Long totalPrice = rentalFee + insuranceFee + deposit;

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .usagePurpose(request.usagePurpose())
                .rentalFee(rentalFee)
                .deposit(deposit)
                .insuranceFee(insuranceFee)
                .platformFee(platformFee)
                .totalPrice(totalPrice)
                .space(space)
                .user(guest)
                .build();

        reservation = reservationRepository.save(reservation);

        return ReservationConverter.toCreateResult(reservation);
    }

    //예약 가능한 날짜인지 확인
    private void validateDateRange(Space space, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_INVALID_PERIOD);
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_INVALID_DATE);
        }
        if (startDate.isBefore(space.getAvailableStartDate()) || endDate.isAfter(space.getAvailableEndDate())) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_INVALID_DATE);
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_RESERVATION_DAYS) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_PERIOD_EXCEEDED);
        }
    }

    //예약 승인(호스트)
    public ReservationResDTO.ReservationStatusChangeRes approveReservation(Long reservationId, Long hostId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateHost(reservation, hostId);
            validateModifiable(reservation);

            reservation.approve();
            reservationRepository.saveAndFlush(reservation);
            contractService.createPendingContract(reservation); // 예약 승인 -> 계약 생성

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    //예약 거절(호스트)
    public ReservationResDTO.ReservationStatusChangeRes rejectReservation(Long reservationId, Long hostId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateHost(reservation, hostId);
            validateModifiable(reservation);

            reservation.reject();
            reservationRepository.saveAndFlush(reservation);

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    //유효한 호스트인지
    private void validateHost(Reservation reservation, Long hostId) {
        if (!reservation.getSpace().getHostId().equals(hostId)) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }

    //승인 대기 상태가 맞는지
    private void validateModifiable(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING_APPROVAL) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
        }
    }

    //예약 취소(게스트)
    public ReservationResDTO.ReservationStatusChangeRes cancelByGuest(Long reservationId, Long guestId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateGuest(reservation, guestId);
            validateGuestCancelable(reservation);

            reservation.cancel();
            reservationRepository.saveAndFlush(reservation);
            // 결제 전 상태(PENDING/APPROVED)에서만 오는 경로라 환불 로직은 없음

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    //유효한 게스트가 맞는지
    private void validateGuest(Reservation reservation, Long guestId) {
        if (!reservation.getUser().getUserId().equals(guestId)) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }
    }

    //게스트가 예약 취소 가능한 상태인지
    private void validateGuestCancelable(Reservation reservation) {
        ReservationStatus status = reservation.getStatus();
        if (status != ReservationStatus.PENDING_APPROVAL && status != ReservationStatus.APPROVED) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CANCEL_NOT_ALLOWED);
        }
    }

    //퇴실 증빙 제출 - 최초 제출 또는 거절 후 재제출만 허용, 사진 최소 1장 필수
    public ReservationResDTO.ReservationStatusChangeRes submitCheckout(
            Long reservationId, Long guestId, ReservationReqDTO.ReservationCheckoutReq request
    ) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateGuest(reservation, guestId);
            if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }
            // 이미 유효한 제출이 있고(호스트가 아직 거절하지 않음) 대기 중이면 재제출 불가
            if (reservation.getCheckoutSubmittedAt() != null && !reservation.getCheckoutRejected()) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_CHECKOUT_ALREADY_SUBMITTED);
            }
            if (request.photoUrls() == null || request.photoUrls().isEmpty()) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_CHECKOUT_PHOTO_REQUIRED);
            }

            List<CheckoutImage> images = IntStream.range(0, request.photoUrls().size())
                    .mapToObj(i -> CheckoutImage.builder()
                            .checkoutImageUrl(request.photoUrls().get(i))
                            .sortOrder(i)
                            .reservation(reservation)
                            .build())
                    .toList();
            checkoutImageRepository.saveAll(images);

            reservation.markCheckoutSubmitted();
            reservationRepository.saveAndFlush(reservation);

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    //퇴실 승인(호스트) - 유효한 제출이 존재하고 거절 상태가 아닐 때만 승인 가능
    public ReservationResDTO.ReservationStatusChangeRes approveCheckout(Long reservationId, Long hostId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateHost(reservation, hostId);
            if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }
            if (reservation.getCheckoutRejected()) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }
            if (reservation.getCheckoutSubmittedAt() == null) {
                // 증빙 미제출 상태에서는 수동 승인 불가 - 24시간 경과 시 스케줄러가 자동 처리
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }

            reservation.completeCheckout();
            reservationRepository.saveAndFlush(reservation);

            try {
                paymentService.settleByReservation(reservationId);
            } catch (ProjectException e) {
                // 정산 일부 실패는 퇴실 승인 자체를 막지 않음
                // 실패한 단계는 Payment에 이미 기록되어 있어 별도로 재시도할 수 있음
                log.warn("퇴실 승인 후 정산 처리 중 일부 실패: reservationId={}", reservationId, e);
            }

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    //퇴실 거절(호스트) - 게스트에게 재인증 요청. 재제출(checkoutRejected=false) 전까지는 중복 거절 불가
    public ReservationResDTO.ReservationStatusChangeRes rejectCheckout(Long reservationId, Long hostId) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

            validateHost(reservation, hostId);
            if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) {
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }
            if (reservation.getCheckoutSubmittedAt() == null) {
                // 아직 제출된 증빙 자체가 없는데 거절할 수는 없음
                throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
            }
            if (reservation.getCheckoutRejected()) {
                // 이미 거절 상태 - 재제출 없는 중복 거절로 24h 타임아웃이 계속 연장되는 것을 방지
                throw new ProjectException(ReservationErrorCode.RESERVATION_CHECKOUT_ALREADY_REJECTED);
            }

            checkoutImageRepository.deleteAllByReservationId(reservationId);
            reservation.rejectCheckout();
            reservationRepository.saveAndFlush(reservation);

            return ReservationConverter.toStatusChange(reservation);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CONCURRENT_MODIFICATION);
        }
    }

    // ===== 스케줄러 전용 - 예약 1건당 독립 트랜잭션으로 처리해 낙관적 락 충돌이 다른 건에 영향 없게 함 =====

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void startUsageForSchedule(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != ReservationStatus.CONTRACT_COMPLETED) return; // 이미 처리됨
        reservation.startUsage();
        reservationRepository.saveAndFlush(reservation);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void completeUsageForSchedule(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != ReservationStatus.IN_USE) return;
        reservation.completeUsage();
        reservationRepository.saveAndFlush(reservation);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void completeCheckoutForSchedule(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) return;
        if (reservation.getCheckoutRejected()) return; // 조회~처리 사이 호스트가 거절했으면 자동승인 스킵
        reservation.completeCheckout();
        reservationRepository.saveAndFlush(reservation);

        try {
            paymentService.settleByReservation(reservationId);
        } catch (ProjectException e) {
            // 정산 일부 실패는 퇴실 자동 승인 자체를 막지 않음
            // 실패한 단계는 Payment에 이미 기록되어 있어 별도로 재시도할 수 있음
            log.warn("퇴실 자동 승인 후 정산 처리 중 일부 실패: reservationId={}", reservationId, e);
        }
    }

    // 퇴실 거절 후 게스트 재제출 없이 24h 경과 - 거절 시각 기준 자동승인
    // (checkoutRejected=true 건 전용. 그 사이 게스트가 재제출했으면(=false로 전환) 스킵하고,
    //  해당 건은 재제출 시각 기준 24h로 completeCheckoutForSchedule 쪽 큐에서 별도로 처리됨)
    // cutoff를 인자로 받아 조회~처리 사이 재거절 등으로 checkoutRejectedAt이 갱신됐다면 다시 스킵되도록 재검증
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void completeCheckoutForRejectedSchedule(Long reservationId, LocalDateTime cutoff) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) return;
        if (!reservation.getCheckoutRejected()) return; // 조회~처리 사이 게스트가 재제출했으면 스킵
        if (reservation.getCheckoutRejectedAt() == null
                || !reservation.getCheckoutRejectedAt().isBefore(cutoff)) {
            return; // 조회~처리 사이 재거절되어 거절 시각이 갱신됐으면(=아직 24h 안 지남) 스킵
        }

        reservation.completeCheckout();
        reservationRepository.saveAndFlush(reservation);

        try {
            paymentService.settleByReservation(reservationId);
        } catch (ProjectException e) {
            log.warn("퇴실 자동 승인(거절 후 재제출 타임아웃) 후 정산 처리 중 일부 실패: reservationId={}", reservationId, e);
        }
    }
}
