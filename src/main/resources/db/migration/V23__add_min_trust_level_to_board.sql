-- 게시판 작성 최소 신뢰 등급(트러스트 게이팅).
-- 기본 'UNVERIFIED' = 게이트 없음(누구나 작성) → 기존 게시판 동작 변화 없음.
-- 값: UNVERIFIED / STUDENT / EXCHANGE_VERIFIED (verification BC TrustLevel name).
ALTER TABLE board
    ADD COLUMN min_trust_level VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED';
