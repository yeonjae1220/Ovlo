package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MediaFileJpaEntity;
import me.yeonjae.ovlo.domain.media.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

@Component
public class MediaFileMapper {

    public MediaFileJpaEntity toJpaEntity(MediaFile mediaFile) {
        MediaFileJpaEntity entity = new MediaFileJpaEntity();
        if (mediaFile.getId() != null) {
            entity.setId(mediaFile.getId().value());
        }
        entity.setUploaderId(mediaFile.getUploaderId().value());
        entity.setMediaType(mediaFile.getMediaType());
        entity.setStoragePath(mediaFile.getStoragePath().path());
        entity.setStorageType(mediaFile.getStoragePath().storageType());
        entity.setOriginalFilename(mediaFile.getOriginalFilename());
        entity.setFileSize(mediaFile.getFileSize());
        return entity;
    }

    public MediaFile toDomain(MediaFileJpaEntity entity) {
        return MediaFile.restore(
                new MediaId(entity.getId()),
                new MemberId(entity.getUploaderId()),
                entity.getMediaType(),
                new StoragePath(entity.getStoragePath(), entity.getStorageType()),
                entity.getOriginalFilename(),
                entity.getFileSize()
        );
    }
}
