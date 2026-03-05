-- ChatRoom 테이블
CREATE TABLE chat_room (
    id          BIGSERIAL   PRIMARY KEY,
    type        VARCHAR(20) NOT NULL,
    name        VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ChatRoom 참여자 테이블 (N:M)
CREATE TABLE chat_room_participant (
    chat_room_id    BIGINT NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
    member_id       BIGINT NOT NULL,
    PRIMARY KEY (chat_room_id, member_id)
);

-- Message 테이블
CREATE TABLE message (
    id              BIGSERIAL   PRIMARY KEY,
    chat_room_id    BIGINT      NOT NULL REFERENCES chat_room(id) ON DELETE CASCADE,
    sender_id       BIGINT      NOT NULL,
    content         TEXT        NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 채팅방 메시지 조회 (최신순)
CREATE INDEX idx_message_chat_room_id ON message (chat_room_id, sent_at DESC);

-- 참여자별 채팅방 조회
CREATE INDEX idx_chat_room_participant_member_id ON chat_room_participant (member_id);
