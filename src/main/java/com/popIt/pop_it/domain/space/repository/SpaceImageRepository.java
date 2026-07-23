package com.popIt.pop_it.domain.space.repository;

import com.popIt.pop_it.domain.space.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpaceImageRepository extends JpaRepository<SpaceImage, Long> {

    // spaceId별 대표(sortOrder 최솟값) 사진만 배치로 조회
    @Query("""
        select si from SpaceImage si
        where si.space.id in :spaceIds
        and si.sortOrder = (
            select min(si2.sortOrder) from SpaceImage si2 where si2.space.id = si.space.id
        )
        """)
    List<SpaceImage> findThumbnailsBySpaceIds(@Param("spaceIds") List<Long> spaceIds);

    // 공간 상세 조회용 - 노출 순서(sortOrder)대로 이미지 URL만 조회
    @Query("""
        select si.imageUrl 
        from SpaceImage si
        where si.space.id = :spaceId
        order by si.sortOrder asc
        """)
    List<String> findImageUrlsBySpaceId(@Param("spaceId") Long spaceId);
}
