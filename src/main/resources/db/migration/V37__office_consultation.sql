-- V37: 상담 이관 (office_consultation) 과 상담 메시지 번역 (chat_message_translation)
-- ============================================================================
-- docs/intl-office-pilot-design.md §4.5, §5.3
-- - 상담 대화는 chat_room / message 에 그대로 저장되고 여기는 상태·시각·동의 기록만 둔다
-- - 동의 없이는 행을 만들 수 없다 (consent_version NOT NULL, consent_text NOT NULL)
-- - 메시지 번역은 (message_id, lang) 당 1건
-- ============================================================================

CREATE TABLE office_consultation (
    id                        BIGSERIAL    PRIMARY KEY,
    office_id                 BIGINT       NOT NULL REFERENCES office(id),
    member_id                 BIGINT       NOT NULL REFERENCES member(id),
    inquiry_id                BIGINT       REFERENCES office_inquiry(id),
    consent_version           VARCHAR(10)  NOT NULL,
    consent_text              TEXT         NOT NULL,
    shared_question           VARCHAR(1000) NOT NULL,
    shared_answer             TEXT,
    shared_lang               VARCHAR(5)   NOT NULL,
    shared_verified_student   BOOLEAN      NOT NULL DEFAULT FALSE,
    status                    VARCHAR(20)  NOT NULL,          -- OPEN | IN_PROGRESS | RESOLVED | CLOSED
    assigned_staff_member_id  BIGINT       REFERENCES member(id),
    chat_room_id              BIGINT       NOT NULL REFERENCES chat_room(id),
    opened_at                 TIMESTAMPTZ  NOT NULL,
    first_staff_reply_at      TIMESTAMPTZ,
    resolved_at               TIMESTAMPTZ,
    closed_at                 TIMESTAMPTZ,

    CONSTRAINT uq_office_consultation_room UNIQUE (chat_room_id)
);

CREATE INDEX idx_office_consultation_office_status ON office_consultation (office_id, status, opened_at DESC);
CREATE INDEX idx_office_consultation_member ON office_consultation (member_id, opened_at DESC);
CREATE INDEX idx_office_consultation_resolved ON office_consultation (status, resolved_at);

CREATE TABLE chat_message_translation (
    id           BIGSERIAL   PRIMARY KEY,
    message_id   BIGINT      NOT NULL REFERENCES message(id) ON DELETE CASCADE,
    lang         VARCHAR(5)  NOT NULL,
    text         TEXT        NOT NULL,
    model_id     VARCHAR(100),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_chat_message_translation UNIQUE (message_id, lang)
);
