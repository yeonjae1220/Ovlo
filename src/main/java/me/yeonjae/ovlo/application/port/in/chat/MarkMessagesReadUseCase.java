package me.yeonjae.ovlo.application.port.in.chat;

public interface MarkMessagesReadUseCase {
    void markRead(Long chatRoomId, Long memberId);
}
