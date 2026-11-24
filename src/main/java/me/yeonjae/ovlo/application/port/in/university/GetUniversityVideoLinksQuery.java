package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;

public interface GetUniversityVideoLinksQuery {

    /** 대학(대표 행 기준, 숨긴 중복 행 포함)에 연결된 영상 링크 — 최신 게시순. */
    PageResult<VideoLinkResult> getVideoLinks(long globalUnivId, int page, int size);
}
