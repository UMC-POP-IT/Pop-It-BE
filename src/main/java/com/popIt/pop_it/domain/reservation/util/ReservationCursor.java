package com.popIt.pop_it.domain.reservation.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 커서 기반 페이징용 - "createdAt:reservationId" 복합 커서 인코딩/디코딩
public class ReservationCursor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public record Decoded(LocalDateTime createdAt, Long id) {}

    public static Decoded decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Decoded(null, null);
        }
        String[] parts = cursor.split(":", 2);
        return new Decoded(LocalDateTime.parse(parts[0], FORMATTER), Long.parseLong(parts[1]));
    }

    public static String encode(LocalDateTime createdAt, Long id) {
        return createdAt.format(FORMATTER) + ":" + id;
    }
}
