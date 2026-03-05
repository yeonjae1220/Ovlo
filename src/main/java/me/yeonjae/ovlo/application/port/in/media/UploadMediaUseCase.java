package me.yeonjae.ovlo.application.port.in.media;

import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaResult;

public interface UploadMediaUseCase {
    MediaResult upload(UploadMediaCommand command);
}
