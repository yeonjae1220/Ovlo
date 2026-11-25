package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/** nextBefore 가 null 이면 마지막 페이지다. */
public record BlockedMemberPageResult(List<BlockedMemberResult> content, Long nextBefore) {}
