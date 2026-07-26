package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.SpaceVisitLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceVisitLogRepository extends JpaRepository<SpaceVisitLog, Long> {

    // 오늘 이미 방문 기록이 있는지 확인 - 같은 유저의 재방문은 UV에 반영하지 않음
    boolean existsBySpaceIdAndUserIdAndVisitDate(Long spaceId, Long userId, LocalDate visitDate);

    // 배치가 특정 날짜의 공간별 순방문자 수(정확한 distinct count)를 집계할 때 사용
    @Query("""
            select v.spaceId as spaceId, count(distinct v.userId) as uvCount
            from SpaceVisitLog v
            where v.visitDate = :visitDate
            group by v.spaceId
            """)
    List<SpaceUvCount> countDistinctUsersByVisitDate(@Param("visitDate") LocalDate visitDate);

    // 집계가 끝난 원본 방문 로그는 배치가 정리한다
    long deleteByVisitDateBefore(LocalDate cutoff);

    // 공간 삭제 시 고아 로그 방지용 - 삭제되는 공간의 방문 기록을 정리
    void deleteAllBySpaceId(Long spaceId);

    // 유저 탈퇴 시 고아 로그 방지용 - 탈퇴하는 유저의 방문 기록을 정리
    void deleteAllByUserId(Long userId);

    interface SpaceUvCount {
        Long getSpaceId();
        Long getUvCount();
    }
}
