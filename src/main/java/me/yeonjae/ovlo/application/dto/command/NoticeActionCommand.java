package me.yeonjae.ovlo.application.dto.command;

/** 승인·보관·재번역처럼 대상만 필요한 명령. actor 는 승인자 기록용(담당자 memberId 문자열). */
public record NoticeActionCommand(Long officeId, Long noticeId, String actor) {
}
