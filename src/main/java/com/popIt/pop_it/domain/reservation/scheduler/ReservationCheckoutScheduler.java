package com.popIt.pop_it.domain.reservation.scheduler;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import com.popIt.pop_it.domain.reservation.service.ReservationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//호스트가 승인 안 하면 퇴실 시간 기준 24h 후 시스템이 자동 승인하는 기능 및
// 시간 변화에 따른 상태변환을 구현하기 위한 스케줄러입니다.
// 예약 1건당 별도 트랜잭션(REQUIRES_NEW)으로 처리해, 한 건이 낙관적 락 충돌로 실패해도
// 나머지 예약 처리에 영향 없도록 함
public class ReservationCheckoutScheduler {
    private final ReservationRepository reservationRepository;
    private final ReservationCommandService reservationCommandService;

    // 1. 이용 시작일 도래 → 사용중(IN_USE) 자동 전환
    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    public void startUsagePeriod() {
        List<Reservation> targets = reservationRepository
                .findAllByStatusAndStartDateLessThanEqual(ReservationStatus.CONTRACT_COMPLETED, LocalDate.now());

        for (Reservation reservation : targets) {
            try {
                reservationCommandService.startUsageForSchedule(reservation.getId());
                log.info("이용 시작일 도래 - 사용중 처리 - reservationId: {}", reservation.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("사용중 처리 중 낙관적 락 충돌 - reservationId: {}", reservation.getId());
            }
        }
    }

    // 2. 이용 기간 종료 → 이용완료(USAGE_COMPLETED) 자동 전환 (종료일 다음날 00:00 기준)
    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    public void completeUsagePeriod() {
        List<Reservation> targets = reservationRepository
                .findAllByStatusAndEndDateBefore(ReservationStatus.IN_USE, LocalDate.now());

        for (Reservation reservation : targets) {
            try {
                reservationCommandService.completeUsageForSchedule(reservation.getId());
                log.info("이용 기간 종료 - 이용완료 처리 - reservationId: {}", reservation.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("이용완료 처리 중 낙관적 락 충돌 - reservationId: {}", reservation.getId());
            }
        }
    }

    // 3. 퇴실 승인 24h 자동 처리 (사진 제출했든 스킵했든 둘 다 커버)
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분마다
    public void autoApproveCheckouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // 사진 제출한 경우(거절된 적 없는 정상 대기) - 제출 시각 기준 24h
        List<Reservation> submitted = reservationRepository
                .findAllByStatusAndCheckoutRejectedFalseAndCheckoutSubmittedAtBefore(ReservationStatus.USAGE_COMPLETED, cutoff);

        // 사진 스킵한 경우(거절된 적 없음) - USAGE_COMPLETED 전환 시점(endDate 다음날 00:00) 기준 24h
        // = endDate가 (오늘 - 2일) 이하인 예약
        LocalDate cutoffDate = LocalDate.now().minusDays(2);
        List<Reservation> skipped = reservationRepository
                .findAllByStatusAndCheckoutRejectedFalseAndCheckoutSubmittedAtIsNullAndEndDateLessThanEqual(ReservationStatus.USAGE_COMPLETED, cutoffDate);

        // 호스트가 퇴실 거절했는데 게스트가 재제출 안 한 경우 - 거절 시각 기준 24h
        List<Reservation> rejectedTimeout = reservationRepository
                .findAllByStatusAndCheckoutRejectedTrueAndCheckoutRejectedAtBefore(ReservationStatus.USAGE_COMPLETED, cutoff);

        submitted.forEach(r -> {
            try {
                reservationCommandService.completeCheckoutForSchedule(r.getId());
                log.info("퇴실 자동 승인(증빙 제출됨) - reservationId: {}", r.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("퇴실 자동 승인 중 낙관적 락 충돌 - reservationId: {}", r.getId());
            }
        });
        skipped.forEach(r -> {
            try {
                reservationCommandService.completeCheckoutForSchedule(r.getId());
                log.info("퇴실 자동 승인(증빙 스킵) - reservationId: {}", r.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("퇴실 자동 승인 중 낙관적 락 충돌 - reservationId: {}", r.getId());
            }
        });
        rejectedTimeout.forEach(r -> {
            try {
                reservationCommandService.completeCheckoutForRejectedSchedule(r.getId());
                log.info("퇴실 자동 승인(거절 후 재제출 타임아웃) - reservationId: {}", r.getId());
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("퇴실 자동 승인 중 낙관적 락 충돌 - reservationId: {}", r.getId());
            }
        });
    }
}

