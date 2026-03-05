-- Follow 테이블 (follower_id + followee_id 복합 고유 키)
CREATE TABLE follow (
    id                  BIGSERIAL PRIMARY KEY,
    follower_id         BIGINT          NOT NULL,
    followee_id         BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_follow UNIQUE (follower_id, followee_id),
    CONSTRAINT chk_no_self_follow CHECK (follower_id != followee_id)
);

-- 팔로워 조회 (followee_id 기준)
CREATE INDEX idx_follow_followee_id ON follow (followee_id);

-- 팔로잉 조회 (follower_id 기준)
CREATE INDEX idx_follow_follower_id ON follow (follower_id);
