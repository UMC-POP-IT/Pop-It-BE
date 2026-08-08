-- 유니크 제약이 JPA 엔티티(@UniqueConstraint)에는 선언돼 있었으나
-- Flyway 마이그레이션에 누락되어 DB 레벨에 적용되지 않은 3개 제약을 추가한다.
-- ddl-auto=validate 환경에서는 Hibernate가 스키마를 생성하지 않으므로 Flyway로 명시적으로 추가한다.

-- 1. wishlist: 같은 유저가 같은 공간을 중복 찜하지 못하게 막음
--    WishlistServiceImpl의 DataIntegrityViolationException 기반 동시 중복 방어가 실제로 동작하려면
--    DB 레벨 제약이 반드시 있어야 한다.
ALTER TABLE wishlist
    ADD CONSTRAINT uk_user_space UNIQUE (user_id, space_id);

-- 2. users: 같은 소셜 계정(provider + uid 조합)으로 중복 가입하지 못하게 막음
ALTER TABLE users
    ADD CONSTRAINT uk_users_social_provider_social_uid UNIQUE (social_provider, social_uid);

-- 3. user_agreement: 같은 유저가 같은 약관에 중복 동의 행을 생성하지 못하게 막음
ALTER TABLE user_agreement
    ADD CONSTRAINT uk_user_term UNIQUE (user_id, term_id);

-- 롤백:
-- ALTER TABLE wishlist DROP INDEX uk_user_space;
-- ALTER TABLE users DROP INDEX uk_users_social_provider_social_uid;
-- ALTER TABLE user_agreement DROP INDEX uk_user_term;
