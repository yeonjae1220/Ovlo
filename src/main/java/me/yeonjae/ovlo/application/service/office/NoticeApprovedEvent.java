package me.yeonjae.ovlo.application.service.office;

/** 승인 트랜잭션이 커밋된 뒤 번역을 시작하라는 신호. */
public record NoticeApprovedEvent(Long noticeId) {
}
