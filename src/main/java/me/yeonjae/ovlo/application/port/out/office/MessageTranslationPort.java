package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;
import java.util.Map;

/** 상담 메시지 번역 저장. 원문은 message.content 그대로이고 번역은 (messageId, lang) 당 1건. */
public interface MessageTranslationPort {
    /** messageId → 번역문. 없는 메시지는 키가 없다. */
    Map<Long, String> findTranslations(List<Long> messageIds, NoticeLang lang);

    void save(Long messageId, NoticeLang lang, String text, String modelId);
}
