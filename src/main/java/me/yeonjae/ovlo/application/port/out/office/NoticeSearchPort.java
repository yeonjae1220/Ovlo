package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.LocalDate;
import java.util.List;

/**
 * 질문에 대한 근거 후보 검색(ADR-0006). 구현은 1차 걸러내기만 하고, 최종 근거 자격은 호출자가
 * {@link Notice#isGroundingEligible} 로 다시 판정한다.
 * 구현: KeywordNoticeSearchAdapter(기본) / PostgresFtsNoticeSearchAdapter(ovlo.search.provider=postgres).
 */
public interface NoticeSearchPort {
    List<Notice> findCandidates(Long officeId, NoticeLang lang, String question, LocalDate today, int limit);
}
