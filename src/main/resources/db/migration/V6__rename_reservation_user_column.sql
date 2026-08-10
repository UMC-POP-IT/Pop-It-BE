-- Reservation.user 필드의 @JoinColumn에 name이 명시돼 있지 않아,
-- Hibernate 기본 네이밍 규칙(필드명 + "_" + 참조 PK 컬럼명)에 따라
-- User 엔티티의 PK 필드명(userId)까지 그대로 붙어 "user_user_id"라는 어색한 컬럼명으로 생성됐다.
-- @JoinColumn(name = "user_id")를 명시했으므로, 운영 DB 컬럼명도 맞춰준다.
--
-- ddl-auto: update는 기존 컬럼을 리네임하지 않고 새 컬럼을 추가하기만 하므로
-- (기존 데이터가 NULL로 채워진 별개 컬럼이 생겨 데이터 유실/무결성 위반 위험) 명시적 마이그레이션으로 처리한다.

ALTER TABLE reservation
    RENAME COLUMN user_user_id TO user_id;

-- 롤백:
-- ALTER TABLE reservation RENAME COLUMN user_id TO user_user_id;
