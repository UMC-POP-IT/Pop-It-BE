-- 수동 반영용 스크립트 (이 프로젝트엔 아직 Flyway/Liquibase 같은 마이그레이션 도구가 없음).
-- prod DB에 직접 실행해야 하며, ddl-auto: validate가 이 인덱스의 존재를 전제로 하지는 않으므로
-- 실행 순서와 무관하게 안전하게 적용/롤백 가능하다.
--
-- 목적: 같은 contract_id에 대해 status='PENDING'인 payment 행이 동시에 2개 이상
-- 존재하지 못하게 DB 레벨에서 막는다 (partial unique index를 함수형 인덱스로 흉내).
-- MySQL은 partial index 문법(WHERE)을 지원하지 않지만, 8.0.13+의 함수형 인덱스와
-- UNIQUE 인덱스가 NULL 다중값을 허용하는 특성을 조합해 동일한 효과를 낸다:
-- status가 PENDING이 아닌 행은 전부 NULL로 평가되어 서로 충돌하지 않고,
-- PENDING인 행끼리만 같은 contract_id를 가지면 충돌한다.
--
-- 사전 요구사항: MySQL 8.0.13 이상 (RDS 버전 확인 필요).

CREATE UNIQUE INDEX idx_pending_payment
ON payment ((CASE WHEN status = 'PENDING' THEN contract_id ELSE NULL END));

-- 롤백:
-- DROP INDEX idx_pending_payment ON payment;
