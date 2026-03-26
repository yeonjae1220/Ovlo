-- V12: Google OAuth2 소셜 로그인 지원
-- password, hometown, home_university_id, major 관련 컬럼을 NULL 허용으로 변경
-- (PENDING_ONBOARDING 상태의 소셜 로그인 회원은 온보딩 완료 전까지 이 값이 없음)

ALTER TABLE member ALTER COLUMN password       DROP NOT NULL;
ALTER TABLE member ALTER COLUMN hometown       DROP NOT NULL;
ALTER TABLE member ALTER COLUMN home_university_id DROP NOT NULL;
ALTER TABLE member ALTER COLUMN major_name     DROP NOT NULL;
ALTER TABLE member ALTER COLUMN degree_type    DROP NOT NULL;
ALTER TABLE member ALTER COLUMN grade_level    DROP NOT NULL;

-- OAuth 제공자 정보 컬럼 추가
ALTER TABLE member
    ADD COLUMN provider    VARCHAR(30)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(255);

-- 기존 회원은 모두 LOCAL 제공자
UPDATE member SET provider = 'LOCAL' WHERE provider IS NULL;

-- 소셜 로그인 회원 빠른 조회를 위한 인덱스
CREATE INDEX idx_member_provider_id ON member (provider, provider_id)
    WHERE provider_id IS NOT NULL;
