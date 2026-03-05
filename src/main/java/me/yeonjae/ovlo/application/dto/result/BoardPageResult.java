package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record BoardPageResult(
        List<BoardResult> content,
        long totalElements,
        int page,
        int size
) {}
