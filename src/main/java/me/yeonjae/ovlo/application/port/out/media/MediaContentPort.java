package me.yeonjae.ovlo.application.port.out.media;

import me.yeonjae.ovlo.domain.media.model.MediaType;

public interface MediaContentPort {
    byte[] validate(byte[] data, MediaType type);
}
