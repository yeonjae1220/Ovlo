package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

public record AskQuestionCommand(Long officeId, Long memberId, String question, NoticeLang lang, Long previousInquiryId) {
    public AskQuestionCommand(Long officeId, Long memberId, String question, NoticeLang lang) {
        this(officeId, memberId, question, lang, null);
    }
}
