package me.yeonjae.ovlo.application.port.out.media;

import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.media.model.StoragePath;

public interface StoragePort {
    StoragePath store(byte[] data, MediaType type, String originalFilename);
    byte[] retrieve(StoragePath path);
    void delete(StoragePath path);
}
