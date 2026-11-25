-- 차단을 중고거래 전용에서 회원 단위로 넓힌다.
-- 기존 marketplace_block 행은 그대로 회원 차단으로 승계된다(중고거래에서 막은 상대는 게시판·DM에서도 막힌다).
ALTER TABLE marketplace_block RENAME TO member_block;
ALTER TABLE member_block ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
-- PK(actor_id, target_id) 는 "내가 막은 사람" 조회를, 이 인덱스는 "나를 막은 사람" 조회를 받친다.
CREATE INDEX idx_member_block_target ON member_block (target_id, actor_id);
-- 롤링 배포 중 아직 교체되지 않은 구버전 파드가 marketplace_block 을 읽고 쓰므로 호환 뷰를 둔다.
-- 단일 테이블 단순 뷰라 INSERT/DELETE 가 그대로 member_block 에 반영된다. 다음 마이그레이션에서 DROP 한다.
CREATE VIEW marketplace_block AS SELECT actor_id, target_id FROM member_block;

-- 게시글·댓글·채팅 메시지·회원에 대한 사용자 신고.
-- target_author_id 는 신고 시점의 작성자를 고정해 둔다(관리자 조치·통계용, 원문이 지워져도 남는다).
CREATE TABLE content_report (
    id               BIGSERIAL PRIMARY KEY,
    target_type      VARCHAR(20)   NOT NULL CHECK (target_type IN ('POST', 'COMMENT', 'CHAT_MESSAGE', 'MEMBER')),
    target_id        BIGINT        NOT NULL,
    target_author_id BIGINT        NOT NULL REFERENCES member(id),
    reporter_id      BIGINT        NOT NULL REFERENCES member(id),
    reason           VARCHAR(30)   NOT NULL CHECK (reason IN ('SPAM', 'HARASSMENT', 'HATE', 'SEXUAL', 'VIOLENCE', 'ILLEGAL', 'PERSONAL_INFO', 'OTHER')),
    detail           VARCHAR(1000),
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACTIONED', 'DISMISSED')),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at      TIMESTAMPTZ,
    resolved_by      VARCHAR(255),
    resolution       VARCHAR(30),
    CHECK (reporter_id <> target_author_id)
);
CREATE INDEX idx_content_report_status ON content_report (status, id DESC);
-- 같은 신고자의 중복 신고는 처리 대기 중일 때만 막는다. 처리(기각·조치)된 뒤 다시 당하면 새로 신고할 수 있어야 한다.
CREATE UNIQUE INDEX uq_content_report_open ON content_report (target_type, target_id, reporter_id) WHERE status = 'OPEN';

-- 관리자가 신고 처리로 내린 채팅 메시지. 탈퇴 숨김(hidden_by_withdrawal)과 사유가 달라 컬럼을 나눈다.
ALTER TABLE message ADD COLUMN hidden_by_moderation BOOLEAN NOT NULL DEFAULT FALSE;
