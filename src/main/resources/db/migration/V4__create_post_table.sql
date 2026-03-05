-- Post 테이블
CREATE TABLE post (
    id                  BIGSERIAL PRIMARY KEY,
    board_id            BIGINT          NOT NULL,
    author_id           BIGINT          NOT NULL,
    title               VARCHAR(200)    NOT NULL,
    content             TEXT            NOT NULL,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    version             BIGINT          NOT NULL DEFAULT 0, -- Optimistic Lock (@Version)
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 게시판별 게시글 조회
CREATE INDEX idx_post_board_id ON post (board_id);

-- 작성자별 게시글 조회
CREATE INDEX idx_post_author_id ON post (author_id);

-- 활성 게시글만 조회 (삭제되지 않은)
CREATE INDEX idx_post_not_deleted ON post (board_id) WHERE deleted = FALSE;

-- 게시글 제목 전문 검색
CREATE INDEX idx_post_title ON post USING GIN (to_tsvector('simple', title));

-- Comment 테이블
CREATE TABLE comment (
    id                  BIGSERIAL PRIMARY KEY,
    post_id             BIGINT          NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    author_id           BIGINT          NOT NULL,
    content             TEXT            NOT NULL,
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 게시글별 댓글 조회
CREATE INDEX idx_comment_post_id ON comment (post_id);

-- Reaction 테이블 (post_id + member_id 복합 고유 키)
CREATE TABLE post_reaction (
    post_id             BIGINT          NOT NULL REFERENCES post(id) ON DELETE CASCADE,
    member_id           BIGINT          NOT NULL,
    reaction_type       VARCHAR(10)     NOT NULL, -- 'LIKE' | 'DISLIKE'
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, member_id)
);

-- 게시글별 반응 수 집계
CREATE INDEX idx_post_reaction_post_id ON post_reaction (post_id);
