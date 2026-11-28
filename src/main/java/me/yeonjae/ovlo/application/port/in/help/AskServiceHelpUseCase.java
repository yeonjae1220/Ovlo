package me.yeonjae.ovlo.application.port.in.help;

import me.yeonjae.ovlo.application.dto.command.AskServiceHelpCommand;
import me.yeonjae.ovlo.application.dto.result.ServiceHelpAnswerResult;

public interface AskServiceHelpUseCase {
    ServiceHelpAnswerResult ask(AskServiceHelpCommand command);
}
