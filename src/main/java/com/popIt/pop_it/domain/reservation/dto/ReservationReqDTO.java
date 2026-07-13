package com.popIt.pop_it.domain.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class ReservationReqDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotNull
        private Long spaceId;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;

        @NotBlank
        private String usagePurpose;
    }
}
