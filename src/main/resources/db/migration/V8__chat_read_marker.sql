CREATE TABLE chat_room_read_marker (
    chat_room_id BIGINT NOT NULL,
    member_id    BIGINT NOT NULL,
    last_read_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (chat_room_id, member_id)
);
