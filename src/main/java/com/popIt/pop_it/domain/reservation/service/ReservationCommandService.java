package com.popIt.pop_it.domain.reservation.service;

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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommandService {

    private static final BigDecimal INSURANCE_RATE = BigDecimal.valueOf(0.05);
    private static final int MAX_RESERVATION_DAYS = 90;

    private final ReservationRepository reservationRepository;
    private final CheckoutImageRepository checkoutImageRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    //예약 요청
    public ReservationResDTO.CreateRes createReservation(Long userId, ReservationReqDTO.CreateReq request) {
        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new ProjectException(SpaceErrorCode.SPACE_NOT_FOUND));

        User guest = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        validateDateRange(space, request.getStartDate(), request.getEndDate());

        boolean overlapping = reservationRepository.existsOverlappingReservation(
                space.getId(), request.getStartDate(), request.getEndDate(),
                List.of(ReservationStatus.CANCELLED)
        );
        if (overlapping) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_ALREADY_TAKEN);
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        Long rentalFee = (long) space.getPricePerDay() * days;
        Long insuranceFee = BigDecimal.valueOf(rentalFee)
                .multiply(INSURANCE_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        Long deposit = space.getDeposit();
        Long totalPrice = rentalFee + insuranceFee + deposit;

        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.PENDING_APPROVAL)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usagePurpose(request.getUsagePurpose())
                .rentalFee(rentalFee)
                .deposit(deposit)
                .insuranceFee(insuranceFee)
                .totalPrice(totalPrice)
                .space(space)
                .user(guest)
                .build();

        reservationRepository.save(reservation);

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
    public ReservationResDTO.StatusChange approveReservation(Long reservationId, Long hostId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        validateHost(reservation, hostId);
        validateModifiable(reservation);

        reservation.approve();
        // TODO: Contract 도메인에 "계약(서명대기) 생성" 요청 필요 - 인터페이스 확정되면 연동

        return ReservationConverter.toStatusChange(reservation);
    }

    //예약 거절(호스트)
    public ReservationResDTO.StatusChange rejectReservation(Long reservationId, Long hostId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        validateHost(reservation, hostId);
        validateModifiable(reservation);

        reservation.reject();

        return ReservationConverter.toStatusChange(reservation);
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
    public ReservationResDTO.StatusChange cancelByGuest(Long reservationId, Long guestId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        validateGuest(reservation, guestId);
        validateGuestCancelable(reservation);

        reservation.cancel();
        // 결제 전 상태(PENDING/APPROVED)에서만 오는 경로라 환불 로직은 없음

        return ReservationConverter.toStatusChange(reservation);
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
    public ReservationResDTO.StatusChange submitCheckout(
            Long reservationId, Long guestId, ReservationReqDTO.Checkout request
    ) {
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
        if (request.getPhotoUrls() == null || request.getPhotoUrls().isEmpty()) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_CHECKOUT_PHOTO_REQUIRED);
        }

        List<CheckoutImage> images = IntStream.range(0, request.getPhotoUrls().size())
                .mapToObj(i -> CheckoutImage.builder()
                        .checkoutImageUrl(request.getPhotoUrls().get(i))
                        .sortOrder(i)
                        .reservation(reservation)
                        .build())
                .toList();
        checkoutImageRepository.saveAll(images);

        reservation.markCheckoutSubmitted();

        return ReservationConverter.toStatusChange(reservation);
    }

    //퇴실 승인(호스트) - 유효한 제출이 존재하고 거절 상태가 아닐 때만 승인 가능
    public ReservationResDTO.StatusChange approveCheckout(Long reservationId, Long hostId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ProjectException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        validateHost(reservation, hostId);
        if (reservation.getStatus() != ReservationStatus.USAGE_COMPLETED) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
        }
        if (reservation.getCheckoutRejected()) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_NOT_MODIFIABLE);
        }

        reservation.completeCheckout();
        // TODO: Payment - 호스트 지급 + 보증금 부분환불 동시 실행

        return ReservationConverter.toStatusChange(reservation);
    }

    //퇴실 거절(호스트) - 게스트에게 재인증 요청, 반복 거절 가능
    public ReservationResDTO.StatusChange rejectCheckout(Long reservationId, Long hostId) {
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

        checkoutImageRepository.deleteAllByReservationId(reservationId);
        reservation.rejectCheckout();
        // TODO: Notification 도메인 - 게스트에게 재인증 요청 알림 발송

        return ReservationConverter.toStatusChange(reservation);
    }
}
