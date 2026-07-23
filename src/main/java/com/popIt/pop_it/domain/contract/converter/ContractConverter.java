package com.popIt.pop_it.domain.contract.converter;

import com.popIt.pop_it.domain.contract.dto.ContractResDTO;
import com.popIt.pop_it.domain.contract.entity.Contract;
import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.global.util.HashUtil;

public class ContractConverter {
    // 결제 예정(게스트) 정보 조회
    public static ContractResDTO.GetGuestContractInfoRes toGetGuestContractInfoRes(Reservation reservation) {
        return ContractResDTO.GetGuestContractInfoRes.builder()
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
    public static ContractResDTO.GetHostContractInfoRes toGetHostContractInfoRes(Reservation reservation) {
        return ContractResDTO.GetHostContractInfoRes.builder()
                .spaceName(reservation.getSpace().getBuildingName())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .period(reservation.getPeriod())
                .rentalFee(reservation.getRentalFee())
                .platformFee(reservation.getPlatformFee())
                .totalPrice(reservation.getHostTotalPrice())
                .build();
    }

    private static final String FIELD_SEPARATOR = "\u001F"; // usagePurpose 등 자유 텍스트와 구분자 충돌 방지용 제어문자

    // 계약 내용 해시 생성
    public static String buildContentHash(Reservation reservation) {
        String payload = String.join(FIELD_SEPARATOR,
                String.valueOf(reservation.getId()), // 예약 ID
                String.valueOf(reservation.getSpace().getId()), // 공간 ID
                reservation.getStartDate().toString(), // LocalDate.toString()은 항상 yyyy-MM-dd 고정 포맷
                reservation.getEndDate().toString(),
                reservation.getUsagePurpose(),
                String.valueOf(reservation.getRentalFee()),
                String.valueOf(reservation.getDeposit()),
                String.valueOf(reservation.getInsuranceFee()),
                String.valueOf(reservation.getPlatformFee()),
                String.valueOf(reservation.getTotalPrice())
        );
        return HashUtil.sha256(payload);
    }

    // 예약 완료 -> 계약 생성; (호스트)계약 서명 대기 상태로 전이
    // 계약 생성 시 Reservation 스냅샷 + contentHash를 한번에 생성
    public static Contract toPendingContract(Reservation reservation) {
        return Contract.builder()
                .reservation(reservation)
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .usagePurpose(reservation.getUsagePurpose())
                .rentalFee(reservation.getRentalFee())
                .deposit(reservation.getDeposit())
                .insuranceFee(reservation.getInsuranceFee())
                .platformFee(reservation.getPlatformFee())
                .totalPrice(reservation.getTotalPrice())
                .contentHash(buildContentHash(reservation)) // 계약 내용 Hash
                .build();
    }
}
