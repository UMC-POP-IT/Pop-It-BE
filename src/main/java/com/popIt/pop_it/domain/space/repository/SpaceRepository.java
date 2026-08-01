package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    // 추천 사유 태그 판별용 - 특정 지역(동) 중심 좌표 계산 재료
    List<Space> findAllByDongAndDeletedAtIsNull(String dong);

    // 추천 사유 태그 판별용 - 특정 지역(동) 평균 대관료 (REGION_CHEAPER_NEARBY 기준가)
    @Query("select avg(s.pricePerDay) from Space s where s.dong = :dong and s.deletedAt is null")
    Double findAvgPricePerDayByDong(@Param("dong") String dong);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // 즉시실패
    @Query("select s from Space s where s.id = :spaceId")
    Optional<Space> findByIdForUpdate(@Param("spaceId") Long spaceId);

    // 소프트 삭제 확인과 락 획득을 하나의 원자적 쿼리로 처리 (조회~락 사이 소프트 삭제 레이스 방지)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0")) // 즉시실패
    @Query("select s from Space s where s.id = :spaceId and s.deletedAt is null")
    Optional<Space> findByIdAndDeletedAtIsNullForUpdate(@Param("spaceId") Long spaceId);

    // 소프트 삭제된 공간은 조회 대상에서 제외
    Optional<Space> findByIdAndDeletedAtIsNull(Long id);

    // 소프트 삭제된 공간은 존재 여부 확인 대상에서 제외
    boolean existsByIdAndDeletedAtIsNull(Long id);

    // 내 공간 목록 조회
    Page<Space> findAllByHostIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long hostId, Pageable pageable);

    // 공간 탐색 (검색, 필터)
    @Query(
            value = """
                    select s
                    from Space s
                    where s.deletedAt is null
                      and (:keyword is null
                           or lower(s.buildingName) like lower(concat('%', :keyword, '%'))
                           or lower(s.district)     like lower(concat('%', :keyword, '%'))
                           or lower(s.dong)         like lower(concat('%', :keyword, '%'))
                           or lower(s.roadAddress)  like lower(concat('%', :keyword, '%'))
                           or (:hasKeywordCategory = true and s.spaceCategory = :keywordCategory)
                           or (:hasKeywordType = true and s.spaceType = :keywordType))
                      and (:district is null or s.district = :district)
                      and (:spaceCategory is null or s.spaceCategory = :spaceCategory)
                    order by s.createdAt desc, s.id desc
                    """,
            countQuery = """
                    select count(s)
                    from Space s
                    where s.deletedAt is null
                      and (:keyword is null
                           or lower(s.buildingName) like lower(concat('%', :keyword, '%'))
                           or lower(s.district)     like lower(concat('%', :keyword, '%'))
                           or lower(s.dong)         like lower(concat('%', :keyword, '%'))
                           or lower(s.roadAddress)  like lower(concat('%', :keyword, '%'))
                           or (:hasKeywordCategory = true and s.spaceCategory = :keywordCategory)
                           or (:hasKeywordType = true and s.spaceType = :keywordType))
                      and (:district is null or s.district = :district)
                      and (:spaceCategory is null or s.spaceCategory = :spaceCategory)
                    """
    )
    Page<Space> search(
            @Param("keyword") String keyword,
            @Param("district") String district,
            @Param("spaceCategory") SpaceCategory spaceCategory,
            @Param("hasKeywordCategory") boolean hasKeywordCategory,
            @Param("keywordCategory") SpaceCategory keywordCategory,
            @Param("hasKeywordType") boolean hasKeywordType,
            @Param("keywordType") SpaceType keywordType,
            Pageable pageable
    );

    /**
     * < 행정동 백필 대상 조회 >
     *
     * 공간 등록, 수정 시 카카오 로컬 API 장애로 dong이 비어 있는 공간을 다시 채우기 위한 조회
     * - 좌표 자체가 잘못돼 영구히 변환되지 않는 공간이 배치를 계속 점유하지 않도록 최근 등록분만 대상으로 한다.
     * - 외부 API 부하를 제한하기 위해 한 번에 최대 50건만 가져온다.
     * - 최근 등록분부터 처리해 새로 실패한 공간이 먼저 복구되도록 한다.
     */
    List<Space> findTop50ByDongIsNullAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAfter);

    @Modifying
    @Query("""
            update Space s
            set s.dong = :dong
            where s.id = :spaceId
                and s.dong is null
                and s.deletedAt is null
                and s.latitude = :latitude
                and s.longitude = :longitude
            """)
    int updateDongIfStillMissing(@Param("spaceId") Long spaceId,
                                 @Param("dong") String dong,
                                 @Param("latitude") Double latitude,
                                 @Param("longitude") Double longitude);

    // 실시간 추천 공간 후보
    @Query("select s from Space s where s.deletedAt is null order by s.createdAt desc, s.id desc")
    List<Space> findAllActiveForRecommendation();
}
