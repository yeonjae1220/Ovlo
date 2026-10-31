package me.yeonjae.ovlo.application.service.office;

import java.time.Instant;

/** 채팅 메시지가 저장됐다. 상담방이면 담당자 첫 답변 시각을 기록하는 데 쓴다. */
public record ChatMessageSentEvent(Long chatRoomId, Long senderId, Instant sentAt) {
}
