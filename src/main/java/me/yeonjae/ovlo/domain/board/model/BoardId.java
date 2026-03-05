package me.yeonjae.ovlo.domain.board.model;

import java.util.Objects;

public record BoardId(Long value) {
    public BoardId {
        Objects.requireNonNull(value, "BoardId는 null일 수 없습니다");
    }
}
