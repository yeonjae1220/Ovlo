package me.yeonjae.ovlo.application.dto.result;

import java.time.Instant;

/**
 * 상담 대화 1건. translatedText 는 요청 언어 번역(원문 그대로면 null). translationFailed 가 true 면 번역을 시도했지만
 * 실패해 원문만 있다 — 빈 값으로 치환하지 않는다(GLOBAL-PIT-108).
 */
public record TranscriptMessageResult(Long messageId, Long senderId, boolean fromStaff, String content,
                                      String translatedText, boolean translationFailed, Instant sentAt) {
}
