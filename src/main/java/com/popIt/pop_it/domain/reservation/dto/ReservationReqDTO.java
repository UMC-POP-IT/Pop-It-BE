package com.popIt.pop_it.domain.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class ReservationReqDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReq {
        @NotNull
        private Long spaceId;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;

        @NotBlank
        private String usagePurpose;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Checkout {
        private List<String> photoUrls; // 여러 장 업로드 가능, 스킵 시 빈 리스트/null
    }
}
