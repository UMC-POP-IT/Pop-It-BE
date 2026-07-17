package com.popIt.pop_it.domain.contract.service;

import com.popIt.pop_it.domain.contract.converter.ContractConverter;
import com.popIt.pop_it.domain.contract.dto.ContractReqDTO;
import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.contract.enums.ContractStatus;
import com.popIt.pop_it.domain.contract.exception.ContractException;
import com.popIt.pop_it.domain.contract.exception.code.ContractErrorCode;
import com.popIt.pop_it.domain.contract.repository.ContractRepository;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.exception.ReservationException;
import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.user.entity.User;
import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    public ContractResDTO.SignatureRes signature(User user, Long reservationId, ContractReqDTO.SignatureReq dto) {

        // 예약 조회
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(()->new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));

        // 사용자의 예약인지 검사
        if ( reservation.getUser() != user ) {
            throw new ReservationException(ReservationErrorCode.RESERVATION_ACCESS_DENIED);
        }

        // 사용자 모드 조희
        UserMode currentMode = user.getCurrentMode();

        // 예약 id로 계약 조회 (예약이 승인되는 시점에 계약이 생성됨)
        Contract contract = contractRepository.findByReservation_Id(reservationId).orElseThrow(() -> new ContractException(ContractErrorCode.CONTRACT_NOT_FOUND));

        // 서명 이미지 저장
        // 호스트가 먼저 서명하고 나서 게스트 서명 가능 -> 계약 COMPLETE
        if (currentMode == UserMode.HOST & contract.getStatus() == ContractStatus.HOST_SIGNATURE_PENDING ) {
            contract = Contract.builder()
                    .status(ContractStatus.GUEST_SIGNATURE_PENDING)
                    .guestSignatureUrl(dto.signatureUrl())
                    .guestSignedAt(LocalDateTime.now())
                    .reservation(reservation)
                    .build();

            contractRepository.save(contract);
        }
        if (currentMode == UserMode.GUEST & contract.getStatus() == ContractStatus.GUEST_SIGNATURE_PENDING) {
            contract = Contract.builder()
                    .status(ContractStatus.COMPLETED)
                    .guestSignatureUrl(dto.signatureUrl())
                    .guestSignedAt(LocalDateTime.now())
                    .reservation(reservation)
                    .build();

            contractRepository.save(contract);
        }

        return ContractResDTO.SignatureRes.builder()
                .contractStatus(contract.getStatus())
                .bothSigned(contract.getStatus() == ContractStatus.COMPLETED)
                .build();
    }
}
