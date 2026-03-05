package me.yeonjae.ovlo.application.dto.command;

import me.yeonjae.ovlo.domain.media.model.MediaType;

public record UploadMediaCommand(
        Long uploaderId,
        String originalFilename,
        MediaType mediaType,
        byte[] data) {
}
