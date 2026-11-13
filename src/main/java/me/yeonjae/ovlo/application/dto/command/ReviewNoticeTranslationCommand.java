package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

public record ReviewNoticeTranslationCommand(Long officeId, Long noticeId, NoticeLang lang, Long revision, Long actor) {
    public ReviewNoticeTranslationCommand(Long officeId,Long noticeId,NoticeLang lang) { this(officeId,noticeId,lang,null,null); }
}
