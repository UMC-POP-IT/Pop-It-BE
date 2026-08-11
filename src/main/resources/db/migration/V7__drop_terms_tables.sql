-- 약관 동의 기능이 실제 서비스 플로우에 연결되지 않은 채 미사용 상태로,
-- 관련 도메인(Terms/UserAgreement 엔티티)을 코드에서 제거하면서
-- 운영 DB에 남는 고아 테이블도 함께 정리한다.
--
-- 두 테이블 간 실제 FK 제약은 없으나(엔티티가 user_id/term_id를 단순 Long으로 보유),
-- 동의 이력(user_agreement)을 먼저 제거하고 약관 원본(terms)을 제거한다.
-- IF EXISTS로 이미 없는 환경에서도 안전하게 통과하도록 한다.

DROP TABLE IF EXISTS user_agreement;
DROP TABLE IF EXISTS terms;

-- 롤백:
-- 두 테이블을 재생성하려면 삭제된 Terms/UserAgreement 엔티티를 복구한 뒤
-- 해당 스키마에 맞는 CREATE TABLE 마이그레이션을 별도로 작성해야 한다.
