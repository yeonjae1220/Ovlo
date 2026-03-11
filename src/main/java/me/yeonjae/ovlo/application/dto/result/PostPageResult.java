package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record PostPageResult(
        List<PostResult> content,
        long totalElements,
        int totalPages,
        int page,
        int size,
        boolean hasNext
) {
    public static PostPageResult of(List<PostResult> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PostPageResult(content, totalElements, totalPages, page, size, (page + 1) < totalPages);
    }
}
