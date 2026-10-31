package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

public record ReviewNoticeTranslationCommand(Long officeId, Long noticeId, NoticeLang lang) {
}
