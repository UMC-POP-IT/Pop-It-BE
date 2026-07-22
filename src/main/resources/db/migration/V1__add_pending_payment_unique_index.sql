-- 같은 contract_id에 대해 status='PENDING'인 payment 행이 동시에 2개 이상
-- 존재하지 못하게 DB 레벨에서 막음 (partial unique index를 함수형 인덱스로 흉내)

CREATE UNIQUE INDEX idx_pending_payment
    ON payment ((CASE WHEN status = 'PENDING' THEN contract_id ELSE NULL END));

-- 롤백:
-- DROP INDEX idx_pending_payment ON payment;