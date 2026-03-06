package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.media.model.MediaFile;

public record MediaResult(
        Long mediaId,
        Long uploaderId,
        String mediaType,
        String storageType,
        String originalFilename,
        long fileSize) {

    public static MediaResult from(MediaFile mediaFile) {
        return new MediaResult(
                mediaFile.getId() != null ? mediaFile.getId().value() : null,
                mediaFile.getUploaderId().value(),
                mediaFile.getMediaType().name(),
                mediaFile.getStoragePath().storageType().name(),
                mediaFile.getOriginalFilename(),
                mediaFile.getFileSize()
        );
    }
}
