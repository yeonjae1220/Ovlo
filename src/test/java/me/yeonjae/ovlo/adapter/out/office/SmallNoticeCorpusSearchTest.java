package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.office.NoticeGovernanceService;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.support.NoticeFixtures;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.stream.LongStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SmallNoticeCorpusSearchTest {
    static final LocalDate TODAY = LocalDate.of(2027, 3, 2);
    static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    final LoadNoticePort load = mock(LoadNoticePort.class);
    final NoticeGovernanceService governance = mock(NoticeGovernanceService.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    NoticeSearchPort adapter(String provider, List<Notice> notices) {
        when(load.findActiveForStudent(5L, null, TODAY)).thenReturn(notices);
        lenient().when(governance.eligible(any(), eq(TODAY))).thenReturn(true);
        return provider.equals("keyword") ? new KeywordNoticeSearchAdapter(load, governance)
                : new PostgresFtsNoticeSearchAdapter(jdbc, load, governance);
    }
    static Notice notice(long id, long office, String title, String body) {
        Notice n = Notice.register(office, NoticeCategory.HOUSING, title, body, null, List.of(), "2027-1",
                TODAY.minusDays(1), TODAY.plusDays(60), 11L, NOW);
        n.assignId(id); n.approve("11", NOW, TODAY); NoticeFixtures.verify(n, NOW); return n;
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void includesAllEightWithoutLexicalOverlapAndUsesStableOrder(String provider) {
        var docs = LongStream.rangeClosed(1, 8).mapToObj(id -> notice(id, 5L, "기숙사", "신청 안내")).toList();
        var search = adapter(provider, docs);
        assertThat(search.findCandidates(5L, NoticeLang.KO, "생활관 입주", TODAY, 8)).extracting(Notice::getId)
                .containsExactly(8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
        assertThat(search.findCandidates(5L, NoticeLang.KO, "?", TODAY, 8)).hasSize(8);
        verifyNoInteractions(jdbc);
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void ninthEligibleNoticeFallsBackToLexicalSearch(String provider) {
        var docs = LongStream.rangeClosed(1, 9).mapToObj(id -> notice(id, 5L, "기숙사", "신청 안내")).toList();
        assertThat(adapter(provider, docs).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        if (provider.equals("postgres")) verify(jdbc).queryForList(anyString(), eq(Long.class), eq("ko"), anyString(), eq(5L), eq(TODAY), eq(TODAY));
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void configuredCandidateLimitIsRespected(String provider) {
        var docs = List.of(notice(1, 5, "기숙사", "신청"), notice(2, 5, "보험", "가입"));
        assertThat(adapter(provider, docs).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 1)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void thirtyThousandUtf8BytesFitButOneMoreFallsBack(String provider) {
        var first = notice(1, 5, "a", "가".repeat(4999));
        var exact = notice(2, 5, "b", "나".repeat(5000) + "x");
        assertThat(adapter(provider, List.of(first, exact)).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).hasSize(2);
        verifyNoInteractions(jdbc);
        var over = notice(2, 5, "b", "나".repeat(5000) + "xx");
        assertThat(adapter(provider, List.of(first, over)).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void budgetUsesRequestedLanguageFullTextBeforeLegacyTruncation(String provider) {
        var shortTranslation = notice(1, 5, "a", "가".repeat(10001));
        shortTranslation.putTranslation(NoticeLang.EN, "Dormitory", "Application", NOW);
        var search = adapter(provider, List.of(shortTranslation));
        assertThat(search.findCandidates(5L, NoticeLang.EN, "unmatchedsynonym", TODAY, 8)).hasSize(1);
        verifyNoInteractions(jdbc);
        assertThat(search.findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        var longTranslation = notice(2, 5, "a", "짧은 공지");
        longTranslation.putTranslation(NoticeLang.EN, "Long guide", "가".repeat(10001), NOW);
        assertThat(adapter(provider, List.of(longTranslation)).findCandidates(5L, NoticeLang.EN, "unmatchedsynonym", TODAY, 8)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void scopeLanguageDomainAndGovernanceEligibilityRemainMandatory(String provider) {
        var eligible = notice(1, 5, "기숙사", "신청"); eligible.putTranslation(NoticeLang.EN, "Dormitory", "Application", NOW);
        var held = notice(2, 5, "기숙사", "신청"); held.holdSource("검토 필요");
        var foreign = notice(3, 99, "기숙사", "신청"); foreign.putTranslation(NoticeLang.EN, "Dormitory", "Application", NOW);
        var noTranslation = notice(4, 5, "보험", "가입");
        var denied = notice(5, 5, "기숙사", "신청"); denied.putTranslation(NoticeLang.EN, "Dormitory", "Application", NOW);
        var expired = notice(6, 5, "기숙사", "신청"); expired.expire(TODAY.plusDays(61));
        var search = adapter(provider, List.of(eligible, held, foreign, noTranslation, denied, expired));
        when(governance.eligible(denied, TODAY)).thenReturn(false);
        assertThat(search.findCandidates(5L, NoticeLang.EN, "unmatchedsynonym", TODAY, 8)).extracting(Notice::getId).containsExactly(1L);
        verify(governance).eligible(denied, TODAY);
        verifyNoInteractions(jdbc);
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void noEligibleDocumentsRemainEmpty(String provider) {
        var held = notice(1, 5, "기숙사", "신청"); held.holdSource("검토 필요");
        assertThat(adapter(provider, List.of(held)).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        verifyNoInteractions(jdbc);
    }
}
