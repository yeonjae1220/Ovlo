package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;

/** 공지의 없거나 stale 인 번역을 생성한다. 게이트에 걸린 언어는 결과로 돌려주고 담당자에게 알린다. */
public interface TranslateNoticeUseCase {
    record Outcome(List<NoticeLang> generated, List<NoticeLang> failed, List<NoticeLang> deferred) {
        public Outcome(List<NoticeLang> generated, List<NoticeLang> failed) { this(generated,failed,List.of()); }
    }

    Outcome translateMissing(Long noticeId);
    Outcome retryManually(Long noticeId);
    void retryDue();
}
