-- 퇴실 거절 시 사진을 하드 삭제하지 않고 비활성화(soft delete)하기 위한 컬럼 추가
-- 기존 행은 전부 하드 삭제 정책 하에서 살아남은(=거절되지 않은) 행이므로 기본값 TRUE로 backfill
-- rejected_at: 여러 번 거절된 경우 가장 최근 거절 배치를 구분하기 위한 시각 (거절 시에만 값 존재)

ALTER TABLE checkout_image
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN rejected_at DATETIME NULL;

-- 롤백:
-- ALTER TABLE checkout_image DROP COLUMN is_active, DROP COLUMN rejected_at;
