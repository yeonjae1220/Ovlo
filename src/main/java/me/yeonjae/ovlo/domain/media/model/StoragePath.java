package me.yeonjae.ovlo.domain.media.model;

import java.util.Objects;

public record StoragePath(String path, StorageType storageType) {

    public StoragePath {
        Objects.requireNonNull(path, "저장 경로는 필수입니다");
        if (path.isBlank()) {
            throw new IllegalArgumentException("저장 경로는 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(storageType, "스토리지 타입은 필수입니다");
    }
}
