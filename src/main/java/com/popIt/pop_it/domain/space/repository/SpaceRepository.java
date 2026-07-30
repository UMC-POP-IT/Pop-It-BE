package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

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
}
