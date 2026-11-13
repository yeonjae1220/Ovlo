package me.yeonjae.ovlo.domain.office.model;

import java.time.LocalDate;

/** 학생에게 공개 가능한 당시의 인용만 보존. 비공개 원문·검토 메모를 넣지 않는다. */
public record NoticeCitation(int index, Long noticeId, int version, String title, String sourceUrl,
                             NoticeSource.Type sourceType, String department, LocalDate verifiedOn,
                             LocalDate expiresAt) {}
