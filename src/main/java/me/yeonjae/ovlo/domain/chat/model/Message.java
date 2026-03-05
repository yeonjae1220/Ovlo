package me.yeonjae.ovlo.domain.chat.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {

    private MessageId id;
    private MemberId senderId;
    private String content;
    private LocalDateTime sentAt;

    private Message() {}

    public static Message create(MemberId senderId, String content) {
        Objects.requireNonNull(senderId, "발신자 ID는 필수입니다");
        Objects.requireNonNull(content, "메시지 내용은 필수입니다");
        if (content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용은 빈 값일 수 없습니다");
        }

        Message message = new Message();
        message.senderId = senderId;
        message.content = content;
        message.sentAt = LocalDateTime.now();
        return message;
    }

    /** persistence 계층 전용 */
    public static Message restore(MessageId id, MemberId senderId, String content, LocalDateTime sentAt) {
        Message message = new Message();
        message.id = id;
        message.senderId = senderId;
        message.content = content;
        message.sentAt = sentAt;
        return message;
    }

    public MessageId getId() { return id; }
    public MemberId getSenderId() { return senderId; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}
