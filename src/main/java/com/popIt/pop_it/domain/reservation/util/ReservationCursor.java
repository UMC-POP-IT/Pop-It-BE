package com.popIt.pop_it.domain.reservation.util;

import com.popIt.pop_it.domain.reservation.exception.code.ReservationErrorCode;
import com.popIt.pop_it.global.apiPayload.exception.ProjectException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 커서 기반 페이징용 - "createdAt|reservationId" 복합 커서 인코딩/디코딩
// 구분자는 "|" - ISO_LOCAL_DATE_TIME 포맷 안에 ":"가 여러 번 등장해서 ":"는 구분자로 쓸 수 없음
public class ReservationCursor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public record Decoded(LocalDateTime createdAt, Long id) {}

    public static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Decoded(null, null);
        }
        String[] parts = cursor.split("\\|", 2);
        if (parts.length != 2) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_INVALID_CURSOR);
        }
        try {
            return new Decoded(LocalDateTime.parse(parts[0], FORMATTER), Long.parseLong(parts[1]));
        } catch (RuntimeException e) {
            throw new ProjectException(ReservationErrorCode.RESERVATION_INVALID_CURSOR);
        }
    }

    public static String encode(LocalDateTime createdAt, Long id) {
        return createdAt.format(FORMATTER) + "|" + id;
    }
}
