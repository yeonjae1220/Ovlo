package me.yeonjae.ovlo.application.dto.result;

/** 내가 차단한 회원을 관리하는 데 필요한 최소 정보만 담는다. */
public record BlockedMemberResult(long memberId, String nickname) {}
