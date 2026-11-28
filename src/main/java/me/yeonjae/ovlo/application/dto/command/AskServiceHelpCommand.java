package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

public record AskServiceHelpCommand(Long memberId, String question, NoticeLang lang) {
}
