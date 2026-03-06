package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

public record PostPageResult(
        List<PostResult> content,
        long totalElements,
        int page,
        int size
) {}
