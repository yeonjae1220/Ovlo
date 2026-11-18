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

    /**
     * 그 학교에 요청 언어로 근거 자격이 있는 공지가 하나라도 있는지. 후보가 0건일 때 "자료가 없다"와
     * "검색이 못 찾았다"를 가르는 진단용이며, 답변 경로의 자격 판정을 대신하지 않는다.
     */
    boolean hasEligibleNotices(Long officeId, NoticeLang lang, LocalDate today);
}
