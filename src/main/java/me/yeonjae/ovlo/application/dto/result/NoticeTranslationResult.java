package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.TranslationStatus;

import java.time.Instant;

/** 담당자 화면용 번역 1건. stale 이면 원문이 바뀐 뒤 아직 다시 번역되지 않은 것. */
public record NoticeTranslationResult(NoticeLang lang, String title, String body, TranslationStatus status,
                                      boolean stale, Instant generatedAt) {
}
