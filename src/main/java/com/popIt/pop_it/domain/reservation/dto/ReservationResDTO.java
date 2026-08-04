package com.popIt.pop_it.domain.reservation.dto;

import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ReservationResDTO {

    @Builder
    @Schema(description = "예약 목록 조회용 (게스트: /me, 호스트: /host)")
    public record ReservationSummaryRes(
            @Schema(description = "예약 ID", example = "1")
            Long reservationId,

            @Schema(description = "예약 상태", example = "PENDING_APPROVAL")
            ReservationStatus status,

            @Schema(description = "예약 상태 설명", example = "승인대기")
            String statusDescription,

            @Schema(description = "이용 시작일", example = "2026-09-01")
            LocalDate startDate,

            @Schema(description = "이용 종료일", example = "2026-09-05")
            LocalDate endDate,

            @Schema(description = "이용 목적 (호스트 목록에만 노출)", example = "팝업스토어 운영", nullable = true)
            String usagePurpose, // 호스트 목록에 노출

            @Schema(description = "총 결제 금액", example = "550000")
            Long totalPrice,

            @Schema(description = "퇴실 증빙 사진 인증 여부. true: 인증 완료, false: 인증 필요", example = "false")
            Boolean isPhotoVerified, // true: 인증 완료, false: 인증 필요

            @Schema(description = "예약 대상 공간 요약 정보")
            ReservationSpaceSummaryRes space,

            @Schema(description = "예약한 게스트 요약 정보. 호스트 목록에서만 채워지고 게스트 목록에선 null", nullable = true)
            ReservationGuestSummaryRes guest
    ) {
    }

    @Builder
    public record ReservationPagedSummaryRes(
            @Schema(description = "예약 목록")
            List<ReservationSummaryRes> reservations,

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            Boolean hasNext,

            @Schema(description = "다음 페이지 조회용 커서. hasNext=false면 null", example = "2026-08-04T10:30:00|42", nullable = true)
            String nextCursor
    ) {
    }

    @Builder
    public record ReservationStatusCountsRes(
            @Schema(description = "예약 상태별 개수")
            Map<ReservationStatus, Long> countsByStatus
    ) {
    }

    @Builder
    public record ReservationSpaceSummaryRes(
            @Schema(description = "공간 ID", example = "10")
            Long spaceId,

            @Schema(description = "건물명", example = "합정 메세나폴리스")
            String buildingName,

            @Schema(description = "주소", example = "서울특별시 마포구 합정동 130-3")
            String address,

            @Schema(description = "대표 사진 URL(sortOrder 최솟값)", example = "https://pop-it-images.s3.ap-northeast-2.amazonaws.com/SPACE_IMAGE/1/uuid1.jpg", nullable = true)
            String thumbnailUrl // 대표 사진(sortOrder 최솟값)
    ) {
    }

    @Builder
    public record ReservationGuestSummaryRes(
            @Schema(description = "게스트 유저 ID", example = "3")
            Long userId,

            @Schema(description = "게스트 닉네임", example = "홍길동")
            String nickname
    ) {
    }

    @Builder
    public record ReservationCreateRes(
            @Schema(description = "생성된 예약 ID", example = "1")
            Long reservationId,

            @Schema(description = "예약 상태 (생성 직후 항상 PENDING_APPROVAL)", example = "PENDING_APPROVAL")
            ReservationStatus status,

            @Schema(description = "예약 상태 설명", example = "승인대기")
            String statusDescription,

            @Schema(description = "대여료 (일 단위 가격 x 이용일수)", example = "450000")
            Long rentalFee,

            @Schema(description = "보증금", example = "50000")
            Long deposit,

            @Schema(description = "보험료 (대여료의 5%)", example = "22500")
            Long insuranceFee,

            @Schema(description = "총 결제 예정 금액 (대여료+보증금+보험료)", example = "522500")
            Long totalPrice
    ) {
    }

    @Builder
    public record ReservationStatusChangeRes(
            @Schema(description = "예약 ID", example = "1")
            Long reservationId,

            @Schema(description = "변경된 예약 상태", example = "APPROVED")
            ReservationStatus status,

            @Schema(description = "예약 상태 설명", example = "승인 완료")
            String statusDescription
    ) {
    }

    @Builder
    public record ReservationUnavailableDatesRes(
            @Schema(description = "예약 불가(선점) 기간 목록")
            List<ReservationDateRangeRes> unavailableDates
    ) {
    }

    @Builder
    public record ReservationDateRangeRes(
            @Schema(description = "선점 시작일", example = "2026-09-01")
            LocalDate startDate,

            @Schema(description = "선점 종료일", example = "2026-09-05")
            LocalDate endDate
    ) {
    }

    @Builder
    public record ReservationCheckoutImagesRes(
            @Schema(description = "퇴실 증빙 사진 URL 목록")
            List<String> photoUrls
    ) {
    }

    @Builder
    @Schema(description = "게스트 본인의 퇴실 증빙 사진 조회용. 거절 상태면 가장 최근 거절 배치, 아니면 현재 유효한 제출 사진을 반환")
    public record ReservationCheckoutPhotosRes(
            @Schema(description = "거절(재인증 대기) 상태 여부", example = "false")
            Boolean checkoutRejected,

            @Schema(description = "퇴실 증빙 사진 URL 목록")
            List<String> photoUrls
    ) {
    }

    @Builder
    @Schema(description = "퇴실 승인 여부 조회용. status/checkoutRejected 조합으로 미제출/대기/거절/승인 완료 상태를 판별")
    public record ReservationCheckoutApprovalRes(
            @Schema(description = "예약 ID", example = "1")
            Long reservationId,

            @Schema(description = "예약 상태. CHECKOUT_COMPLETED면 퇴실 승인 완료", example = "USAGE_COMPLETED")
            ReservationStatus status,

            @Schema(description = "예약 상태 설명", example = "이용 완료")
            String statusDescription,

            @Schema(description = "거절(재인증 대기) 상태 여부", example = "false")
            Boolean checkoutRejected,

            @Schema(description = "퇴실 증빙 제출 일시. 미제출이면 null", example = "2026-09-06T11:00:00", nullable = true)
            LocalDateTime checkoutSubmittedAt,

            @Schema(description = "퇴실 증빙 거절 일시. 거절된 적 없으면 null", example = "2026-09-06T15:00:00", nullable = true)
            LocalDateTime checkoutRejectedAt
    ) {
    }

}
