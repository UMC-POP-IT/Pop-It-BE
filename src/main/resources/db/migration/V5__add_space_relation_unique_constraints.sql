-- 공간 도메인 N:M, 순서 테이블의 무결성 제약을 DB 레벨에 명시적으로 못 박는다.
-- 엔티티에는 @Table(uniqueConstraints = ...)로 선언돼 있지만,
-- hibernate ddl-auto: validate는 테이블, 컬럼만 검사하고 제약, 인덱스는 검사하지 않는다.
-- 즉 "엔티티에 써 있다"와 "운영 DB에 실제로 걸려 있다"는 별개이므로 마이그레이션으로 보장한다.
--
-- 각 제약은 이미 존재하면 건너뛴다.

-- 1) 같은 공간에 같은 시설이 두 번 연결되지 못하게 막는다.
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'space_facility'
      AND index_name = 'uk_space_facility'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE space_facility ADD CONSTRAINT uk_space_facility UNIQUE (space_id, facility_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 같은 공간 안에서 사진 노출 순서(sort_order)가 중복되지 못하게 막는다.
--    대표 이미지는 min(sort_order)로 뽑으므로, 중복되면 대표 이미지가 비결정적으로 정해진다.
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'space_image'
      AND index_name = 'uk_space_image_space_sort'
);
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE space_image ADD CONSTRAINT uk_space_image_space_sort UNIQUE (space_id, sort_order)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 롤백:
-- ALTER TABLE space_facility DROP INDEX uk_space_facility;
-- ALTER TABLE space_image DROP INDEX uk_space_image_space_sort;