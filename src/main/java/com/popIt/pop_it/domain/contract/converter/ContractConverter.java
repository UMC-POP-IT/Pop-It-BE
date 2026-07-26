package com.popIt.pop_it.domain.contract.converter;

import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.reservation.entity.Reservation;

public class ContractConverter {
    // 결제 예정(게스트) 정보 조회
    public static ContractResDTO.ContractGuestInfoRes toGetGuestContractInfoRes(Reservation reservation) {
        return ContractResDTO.ContractGuestInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .period(reservation.getPeriod())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .totalPrice(reservation.getTotalPrice())
                .build();
    }

    // 임대 예정(호스트) 정보 조회
    public static ContractResDTO.ContractHostInfoRes toGetHostContractInfoRes(Reservation reservation) {
        return ContractResDTO.ContractHostInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .period(reservation.getPeriod())
                .rentalFee(reservation.getRentalFee())
                .platformFee(reservation.getPlatformFee())
                .totalPrice(reservation.getHostTotalPrice())
                .build();
    }

    // 예약 완료 -> 계약 생성; (호스트)계약 서명 대기 상태로 전이
    // 계약 생성 시 Reservation 스냅샷 + contentHash를 한번에 생성
    public static Contract toPendingContract(Reservation reservation, String contentHash) {
        return Contract.builder()
                .reservation(reservation)
                .hostId(reservation.getSpace().getHostId())
                .guestId(reservation.getUser().getUserId())
                .spaceId(reservation.getSpace().getId())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .usagePurpose(reservation.getUsagePurpose())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .platformFee(reservation.getPlatformFee())
                .totalPrice(reservation.getTotalPrice())
                .contentHash(contentHash) // 계약 내용 Hash
                .build();
    }
}
