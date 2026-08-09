-- 유니크 제약이 JPA 엔티티(@UniqueConstraint)에는 선언돼 있었으나
-- Flyway 마이그레이션에 누락되어 DB 레벨에 적용되지 않은 3개 제약을 추가한다.
-- ddl-auto=validate 환경에서는 Hibernate가 스키마를 생성하지 않으므로 Flyway로 명시적으로 추가한다.
--
-- 제약 추가 전 각 테이블의 중복 행을 먼저 제거한다.
-- 중복이 있는 경우 가장 오래된 행(MIN(id))을 남기고 나머지를 삭제한다.
-- 중복이 없으면 DELETE 대상이 0건이므로 영향 없다.

-- 1. wishlist: 중복 행 제거 후 유니크 제약 추가
DELETE FROM wishlist
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM wishlist
        GROUP BY user_id, space_id
    ) AS tmp
);

ALTER TABLE wishlist
    ADD CONSTRAINT uk_user_space UNIQUE (user_id, space_id);

-- 2. users: 중복 행 제거 후 유니크 제약 추가
DELETE FROM users
WHERE user_id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(user_id) AS min_id
        FROM users
        GROUP BY social_provider, social_uid
    ) AS tmp
);

ALTER TABLE users
    ADD CONSTRAINT uk_users_social_provider_social_uid UNIQUE (social_provider, social_uid);

-- 3. user_agreement: 중복 행 제거 후 유니크 제약 추가
DELETE FROM user_agreement
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM user_agreement
        GROUP BY user_id, term_id
    ) AS tmp
);

ALTER TABLE user_agreement
    ADD CONSTRAINT uk_user_term UNIQUE (user_id, term_id);

-- 롤백:
-- ALTER TABLE wishlist DROP INDEX uk_user_space;
-- ALTER TABLE users DROP INDEX uk_users_social_provider_social_uid;
-- ALTER TABLE user_agreement DROP INDEX uk_user_term;
