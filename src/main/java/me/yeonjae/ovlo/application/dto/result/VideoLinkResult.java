package me.yeonjae.ovlo.application.dto.result;

import java.time.OffsetDateTime;

/** 대학에 연결된 유튜브 영상 링크 — 링크·제목·채널·게시일만. 분석 값은 싣지 않는다. */
public record VideoLinkResult(
        Long id,
        String youtubeUrl,
        String title,
        String channel,
        OffsetDateTime publishedAt
) {}
