-- Board 테이블
CREATE TABLE board (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         TEXT,
    category            VARCHAR(30)     NOT NULL,
    scope               VARCHAR(20)     NOT NULL,
    creator_id          BIGINT          NOT NULL,
    university_id       BIGINT,         -- scope = 'UNIVERSITY' 일 때만 사용
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 게시판 이름 검색 성능 최적화
CREATE INDEX idx_board_name ON board USING GIN (to_tsvector('simple', name));

-- 범위별 필터링
CREATE INDEX idx_board_scope ON board (scope);

-- 대학별 게시판 조회
CREATE INDEX idx_board_university_id ON board (university_id) WHERE university_id IS NOT NULL;

-- 카테고리별 필터링
CREATE INDEX idx_board_category ON board (category);

-- 활성 게시판만 조회
CREATE INDEX idx_board_active ON board (active) WHERE active = TRUE;

-- Board 구독 테이블 (boardId + memberId 복합 고유 키)
CREATE TABLE board_subscription (
    board_id            BIGINT          NOT NULL REFERENCES board(id) ON DELETE CASCADE,
    member_id           BIGINT          NOT NULL,
    subscribed_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    PRIMARY KEY (board_id, member_id)
);

-- 회원의 구독 목록 조회
CREATE INDEX idx_board_subscription_member ON board_subscription (member_id);
