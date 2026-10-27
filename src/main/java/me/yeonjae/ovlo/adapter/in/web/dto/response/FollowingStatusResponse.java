package me.yeonjae.ovlo.adapter.in.web.dto.response;

import java.util.List;

/** 후보 회원 중 요청자가 팔로우 중인 ID 목록 — 회원 정보를 싣지 않는다. */
public record FollowingStatusResponse(List<Long> following) {
}
