package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

/**
 * 공지·메시지 번역(설계 §8). 구현: FakeTranslationAdapter(기본, 결정적) → LLM 어댑터(S3).
 * 실패는 예외로 던진다 — 빈 값으로 치환하지 않는다(GLOBAL-PIT-108).
 */
public interface TranslationPort {

    record Request(NoticeLang from, NoticeLang to, String title, String body) {}

    record Result(String title, String body, String modelId) {}

    Result translate(Request request);
}
