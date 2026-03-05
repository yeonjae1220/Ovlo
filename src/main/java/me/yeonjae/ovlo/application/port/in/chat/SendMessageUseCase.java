package me.yeonjae.ovlo.application.port.in.chat;

import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.dto.result.MessageResult;

public interface SendMessageUseCase {

    MessageResult sendMessage(SendMessageCommand command);
}
