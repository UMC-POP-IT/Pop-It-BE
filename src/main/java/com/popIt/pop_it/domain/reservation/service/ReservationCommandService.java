package com.popIt.pop_it.domain.reservation.service;

import com.popIt.pop_it.domain.reservation.converter.ReservationConverter;
import com.popIt.pop_it.domain.reservation.dto.ReservationReqDTO;
import com.popIt.pop_it.domain.reservation.dto.ReservationResDTO;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationCommandService {

    private static final BigDecimal INSURANCE_RATE = BigDecimal.valueOf(0.05);

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;

    //예약 요청
    public ReservationResDTO.Create createReservation(Long userId, ReservationReqDTO.Create request) {
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
}
