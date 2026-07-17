package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.converter.ContractConverter;
import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.exception.ReservationException;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import com.popIt.pop_it.global.apiPayload.code.GeneralErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ReservationRepository reservationRepository;

    public ContractResDTO.ContractInfoRes getContractInfo(User user, Long reservationId) {

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(()->new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 사용자의 예약인지 검사
        if ( reservation.getUser() != user ) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        // 결제 정보 조회 (예약 정보 조회)
        UserMode currentMode = user.getCurrentMode();
        if (currentMode == UserMode.GUEST) {
            return ContractConverter.toGetGuestContractInfoRes(reservation);
        } else {
            return ContractConverter.toGetHostContractInfoRes(reservation);
        }

    }

    public ContractResDTO.SignatureRes signature(Long reservationId, ContractReqDTO.SignatureReq dto) {
        throw new ProjectException(GeneralErrorCode.FUNCTION_ERROR);
    }
}
