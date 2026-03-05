package me.yeonjae.ovlo.application.port.in.chat;

import me.yeonjae.ovlo.application.dto.command.CreateChatRoomCommand;
import me.yeonjae.ovlo.application.dto.result.ChatRoomResult;

public interface CreateChatRoomUseCase {

    ChatRoomResult createChatRoom(CreateChatRoomCommand command);
}
