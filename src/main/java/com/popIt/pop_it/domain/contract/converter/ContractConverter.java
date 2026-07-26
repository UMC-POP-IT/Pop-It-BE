package com.popIt.pop_it.domain.contract.converter;

import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.reservation.entity.Reservation;

public class ContractConverter {
    // 예약 완료 -> 계약 생성; (호스트)계약 서명 대기 상태로 전이
    // 계약 생성 시 Reservation 스냅샷 + contentHash를 한번에 생성
    public static Contract toPendingContract(Reservation reservation, String contentHash) {
        return Contract.builder()
                .reservation(reservation)
                .hostId(reservation.getSpace().getHostId())
                .guestId(reservation.getUser().getUserId())
                .spaceId(reservation.getSpace().getId())
                .spaceName(reservation.getSpace().getBuildingName())
                .roadAddress(reservation.getSpace().getRoadAddress())
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

    // 결제 예정(게스트) 정보 조회
    public static ContractResDTO.ContractGuestPaymentInfoRes toGetGuestContractPaymentInfoRes(Contract contract) {
        return ContractResDTO.ContractGuestPaymentInfoRes.builder()
                .spaceName(contract.getSpaceName())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .deposit(contract.getDeposit())
                .insuranceFee(contract.getInsuranceFee())
                .totalPrice(contract.getTotalPrice())
                .build();
    }

    // 임대 예정(호스트) 정보 조회
    public static ContractResDTO.ContractHostPaymentInfoRes toGetHostContractPaymentInfoRes(Contract contract) {
        return ContractResDTO.ContractHostPaymentInfoRes.builder()
                .spaceName(contract.getSpaceName())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .platformFee(contract.getPlatformFee())
                .totalPrice(contract.getHostTotalPrice())
                .build();
    }

    // 계약 예정 계약서 조회
    public static ContractResDTO.ContractInfoRes toGetContractInfoRes(Contract contract) {
        return ContractResDTO.ContractInfoRes.builder()
                .spaceName(contract.getSpaceName())
                .roadAddress(contract.getRoadAddress())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .period(contract.getPeriod())
                .rentalFee(contract.getRentalFee())
                .deposit(contract.getDeposit())
                .insuranceFee(contract.getInsuranceFee())
                .build();
    }
}
