-- 같은 space_id에 대해 삭제되지 않은(is_default=true, deleted_at IS NULL) scene 행이
-- 동시에 2개 이상 존재하지 못하게 DB 레벨에서 막음 (partial unique index를 함수형 인덱스로 흉내)

CREATE UNIQUE INDEX idx_default_scene
    ON scene ((CASE WHEN is_default = true AND deleted_at IS NULL THEN space_id ELSE NULL END));

-- 롤백:
-- DROP INDEX idx_default_scene ON scene;
