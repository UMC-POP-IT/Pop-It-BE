package com.popIt.pop_it.domain.reservation.dto;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReservationResDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        // 목록 조회용 (게스트: /me, 호스트: /host)
        private Long reservationId;
        private ReservationStatus status;
        private String statusDescription;
        private LocalDate startDate;
        private LocalDate endDate;
        private String usagePurpose; // 호스트 목록에 노출
        private Long totalPrice;
        private Boolean isPhotoVerified; // true: 인증 완료, false: 인증 필요
        private SpaceSummary space;
        // 호스트 목록에서만 필요 (게스트 목록에선 null)
        private GuestSummary guest;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SpaceSummary {
        private Long spaceId;
        private String buildingName;
        private String address;
        private String thumbnailUrl; // 대표 사진(sortOrder 최솟값)
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GuestSummary {
        private Long userId;
        private String nickname;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Create {
        private Long reservationId;
        private ReservationStatus status;
        private String statusDescription;
        private Long rentalFee;
        private Long deposit;
        private Long insuranceFee;
        private Long totalPrice;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class StatusChange {
        private Long reservationId;
        private ReservationStatus status;
        private String statusDescription;
    }

}
