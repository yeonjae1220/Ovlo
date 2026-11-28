package me.yeonjae.ovlo.application.port.out.moderation;

import java.time.Instant;

/** 보유 기간이 지난 신고 기록을 사람을 알아볼 수 없게 만든다. */
public interface ReportRetentionPort {
    /**
     * 처리 시각(게시글 등 신고) 또는 접수 시각(중고거래 신고)이 cutoff 이하인 기록을 익명화한다.
     * @return 이번에 익명화한 건수(두 종류 합계). 이미 익명화된 기록은 세지 않는다.
     */
    int anonymizeOlderThan(Instant cutoff);
}
