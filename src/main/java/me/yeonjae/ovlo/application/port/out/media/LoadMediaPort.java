package me.yeonjae.ovlo.application.port.out.media;

import me.yeonjae.ovlo.domain.media.model.MediaFile;
import me.yeonjae.ovlo.domain.media.model.MediaId;

import java.util.Optional;

public interface LoadMediaPort {
    Optional<MediaFile> findById(MediaId mediaId);
}
