package com.popIt.pop_it.domain.reservation.scheduler;

import com.popIt.pop_it.domain.reservation.entity.Reservation;
import com.popIt.pop_it.domain.reservation.enums.ReservationStatus;
import com.popIt.pop_it.domain.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//호스트가 승인 안 하면 퇴실 시간 기준 24h 후 시스템이 자동 승인하는 기능 및
// 이용완료로의 상태변환을 구현하기 위한 스케줄러입니다.
public class ReservationCheckoutScheduler {
    private final ReservationRepository reservationRepository;

    // 1. 이용 기간 종료 → 이용완료(USAGE_COMPLETED) 자동 전환
    @Scheduled(cron = "0 0 * * * *") // 매시 정각
    @Transactional
    public void completeUsagePeriod() {
        List<Reservation> targets = reservationRepository
                .findAllByStatusAndEndDateBefore(ReservationStatus.IN_USE, LocalDate.now());

        for (Reservation reservation : targets) {
            reservation.completeUsage();
            log.info("이용 기간 종료 - 이용완료 처리 - reservationId: {}", reservation.getId());
        }
    }

    // 2. 퇴실 승인 24h 자동 처리 (사진 제출했든 스킵했든 둘 다 커버)
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30분마다
    @Transactional
    public void autoApproveCheckouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        // 사진 제출한 경우 - 제출 시각 기준 24h
        List<Reservation> submitted = reservationRepository
                .findAllByStatusAndCheckoutSubmittedAtBefore(ReservationStatus.USAGE_COMPLETED, cutoff);

        // 사진 스킵한 경우 - 이용 종료일 기준 24h
        List<Reservation> skipCandidates = reservationRepository
                .findAllByStatusAndCheckoutSubmittedAtIsNull(ReservationStatus.USAGE_COMPLETED);
        List<Reservation> skipped = skipCandidates.stream()
                .filter(r -> r.getEndDate().atStartOfDay().plusHours(24).isBefore(LocalDateTime.now()))
                .toList();

        submitted.forEach(r -> {
            r.completeCheckout();
            log.info("퇴실 자동 승인(증빙 제출됨) - reservationId: {}", r.getId());
        });
        skipped.forEach(r -> {
            r.completeCheckout();
            log.info("퇴실 자동 승인(증빙 스킵) - reservationId: {}", r.getId());
        });
        // TODO: Escrow - 두 케이스 모두 정산 실행 (approveCheckout과 로직 공통화 필요)
    }
}

