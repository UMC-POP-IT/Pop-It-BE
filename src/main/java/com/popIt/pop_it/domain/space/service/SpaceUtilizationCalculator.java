package com.popIt.pop_it.domain.space.service;

import com.popIt.pop_it.domain.payment.enums.PaymentStatus;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.repository.*;
import com.popIt.pop_it.domain.user_event.entity.enums.UserEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

// 실시간 추천의 가동률 하위 공간 판정
//  - 조건 A : 최근 30일 조회수가 상위 50% 안에 들면서 결제 전환율이 2% 미만
//  - 조건 B : 당월 대여 가능 일수 대비 실제 결제된 일수의 비율이 30% 미만
@Component
@RequiredArgsConstructor
public class SpaceUtilizationCalculator {

    private final SpaceViewCountRepository spaceViewCountRepository;
    private final SpaceUtilizationRepository spaceUtilizationRepository;

    // 조회수, 결제 집계 기간
    private static final int METRIC_WINDOW_DAYS = 30;

    // 조건 A - 결제 전환율 기준 (2% 미만이면 저전환)
    private static final double CONVERSION_RATE_THRESHOLD = 0.02;

    // 조건 B - 예약률 기준 (30% 미만이면 공실률이 높음)
    private static final double OCCUPANCY_RATE_THRESHOLD = 0.30;

    // 가동률 하위 공간 ID 집합 반환
    public Set<Long> findLowUtilizationSpaceIds(List<Space> spaces, LocalDateTime now) {
        if (spaces == null || spaces.isEmpty()) {
            return Set.of();
        }

        List<Long> spaceIds = spaces.stream()
                .map(Space::getId)
                .toList();

        LocalDateTime since = now.minusDays(METRIC_WINDOW_DAYS);
        LocalDate monthStart = now.toLocalDate().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        // 지표를 각각 한 번의 쿼리로 모아서 조회
        Map<Long, Long> viewCountBySpaceId = loadViewCounts(spaceIds, since);
        Map<Long, Long> paidCountBySpaceId = loadPaidCounts(spaceIds, since);
        Map<Long, Long> paidDaysBySpaceId = loadPaidDays(spaceIds, monthStart, monthEnd);

        // 조건 A의 상위 50% 판정을 위한 조회수 기준선
        long viewCountThreshold = calculateTopHalfThreshold(spaceIds, viewCountBySpaceId);

        return spaces.stream()
                .filter(space -> isHighInterestLowConversion(space, viewCountBySpaceId, paidCountBySpaceId, viewCountThreshold)
                        || isHighVacancy(space, paidDaysBySpaceId, monthStart, monthEnd))
                .map(Space::getId)
                .collect(Collectors.toSet());
    }

    // 조건 A - 조회수는 상위 50% 안에 들지만 결제 전환율이 2% 미만인 경우
    private boolean isHighInterestLowConversion(
            Space space,
            Map<Long, Long> viewCountBySpaceId,
            Map<Long, Long> paidCountBySpaceId,
            long viewCountThreshold
    ) {
        long viewCount = viewCountBySpaceId.getOrDefault(space.getId(), 0L);

        if (viewCount <= 0 || viewCount < viewCountThreshold) {
            return false;
        }

        long paidCount = paidCountBySpaceId.getOrDefault(space.getId(), 0L);
        double conversionRate = (double) paidCount / viewCount;

        return conversionRate < CONVERSION_RATE_THRESHOLD;
    }

    // 조건 B - 대여 가능 일수 대비 결제된 일수의 비율이 30% 미만인 경우
    private boolean isHighVacancy(
            Space space,
            Map<Long, Long> paidDaysBySpaceId,
            LocalDate monthStart,
            LocalDate monthEnd
    ) {
        long availableDays = countOverlappingDays(
                space.getAvailableStartDate(), space.getAvailableEndDate(), monthStart, monthEnd);

        if (availableDays <= 0) {
            return false;
        }

        long paidDays = paidDaysBySpaceId.getOrDefault(space.getId(), 0L);
        double occupancyRate = (double) paidDays / availableDays;

        return occupancyRate < OCCUPANCY_RATE_THRESHOLD;
    }

    private Map<Long, Long> loadViewCounts(List<Long> spaceIds, LocalDateTime since) {
        return spaceViewCountRepository
                .countViewsBySpaceIds(UserEventType.VIEW, spaceIds, since).stream()
                .collect(Collectors.toMap(
                        SpaceViewCount::getSpaceId,
                        row -> row.getViewCount() == null ? 0L : row.getViewCount()));
    }

    private Map<Long, Long> loadPaidCounts(List<Long> spaceIds, LocalDateTime since) {
        return spaceUtilizationRepository
                .countPaidBySpaceIds(PaymentStatus.PAID, spaceIds, since).stream()
                .collect(Collectors.toMap(
                        SpacePaidCount::getSpaceId,
                        row -> row.getPaidCount() == null ? 0L : row.getPaidCount()));
    }

    private Map<Long, Long> loadPaidDays(List<Long> spaceIds, LocalDate monthStart, LocalDate monthEnd) {
        List<SpacePaidPeriod> periods = spaceUtilizationRepository.findPaidPeriodsBySpaceIds(
                PaymentStatus.PAID, ReservationStatus.CANCELLED, spaceIds, monthStart, monthEnd);

        Map<Long, Set<LocalDate>> paidDatesBySpaceId = new HashMap<>();

        for (SpacePaidPeriod period : periods) {
            LocalDate from = maxOf(period.getStartDate(), monthStart);
            LocalDate to = minOf(period.getEndDate(), monthEnd);

            if (from.isAfter(to)) {
                continue;
            }

            Set<LocalDate> dates = paidDatesBySpaceId.computeIfAbsent(
                    period.getSpaceId(), key -> new HashSet<>());

            for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
                dates.add(date);
            }
        }

        return paidDatesBySpaceId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));
    }

    private long calculateTopHalfThreshold(List<Long> spaceIds, Map<Long, Long> viewCountBySpaceId) {
        List<Long> sortedDesc = spaceIds.stream()
                .map(spaceId -> viewCountBySpaceId.getOrDefault(spaceId, 0L))
                .sorted(Comparator.reverseOrder())
                .toList();

        int topHalfSize = (int) Math.ceil(sortedDesc.size() / 2.0);

        return sortedDesc.get(topHalfSize - 1);
    }

    private long countOverlappingDays(LocalDate start, LocalDate end, LocalDate periodStart, LocalDate periodEnd) {
        if (start == null || end == null) {
            return 0L;
        }

        LocalDate from = maxOf(start, periodStart);
        LocalDate to = minOf(end, periodEnd);

        if (from.isAfter(to)) {
            return 0L;
        }

        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    private LocalDate maxOf(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate minOf(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
