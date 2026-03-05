package me.yeonjae.ovlo.domain.media.model;

import java.util.Objects;

public record MediaId(Long value) {

    public MediaId {
        Objects.requireNonNull(value, "MediaId는 null일 수 없습니다");
    }
}
