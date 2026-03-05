package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record UniversityPageResult(
        List<UniversityResult> content,
        long totalElements,
        int page,
        int size
) {}
