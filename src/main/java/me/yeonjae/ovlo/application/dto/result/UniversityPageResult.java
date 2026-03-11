package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record UniversityPageResult(
        List<UniversityResult> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean hasNext
) {
    public static UniversityPageResult of(List<UniversityResult> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new UniversityPageResult(content, totalElements, totalPages, page, size, (page + 1) < totalPages);
    }
}
