package me.yeonjae.ovlo.application.port.out.media;

import me.yeonjae.ovlo.domain.media.model.MediaFile;

public interface SaveMediaPort {
    MediaFile save(MediaFile mediaFile);
    void delete(MediaFile mediaFile);
}
