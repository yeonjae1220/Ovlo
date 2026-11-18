package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;

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
    /** Existing cases keep the bypass ceiling at the ranked limit so their boundaries stay exact. */
    NoticeSearchPort adapter(String provider, List<Notice> notices) { return adapter(provider, notices, 8); }
    NoticeSearchPort adapter(String provider, List<Notice> notices, int smallCorpusLimit) {
        when(load.findActiveForStudent(5L, null, TODAY)).thenReturn(notices);
        lenient().when(governance.eligible(any(), eq(TODAY))).thenReturn(true);
        return provider.equals("keyword") ? new KeywordNoticeSearchAdapter(load, governance, new OfficeMetrics(new SimpleMeterRegistry()), smallCorpusLimit)
                : new PostgresFtsNoticeSearchAdapter(jdbc, load, governance, new OfficeMetrics(new SimpleMeterRegistry()), smallCorpusLimit);
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
    void bypassCeilingIsIndependentOfTheRankedLimit(String provider) {
        var docs = LongStream.rangeClosed(1, 30).mapToObj(id -> notice(id, 5L, "기숙사", "신청 안내")).toList();
        assertThat(adapter(provider, docs, 30).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8))
                .extracting(Notice::getId).hasSize(30).startsWith(30L, 29L).endsWith(2L, 1L);
        verifyNoInteractions(jdbc);
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void oneMoreThanTheBypassCeilingFallsBackToRankedSearchWithItsOwnLimit(String provider) {
        var docs = LongStream.rangeClosed(1, 31).mapToObj(id -> notice(id, 5L, "기숙사", "신청 안내")).toList();
        var search = adapter(provider, docs, 30);
        assertThat(search.findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        if (provider.equals("keyword")) assertThat(search.findCandidates(5L, NoticeLang.KO, "기숙사 신청", TODAY, 8)).hasSize(8);
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void configuredBypassCeilingIsRespectedAndZeroDisablesIt(String provider) {
        var docs = List.of(notice(1, 5, "기숙사", "신청"), notice(2, 5, "보험", "가입"));
        assertThat(adapter(provider, docs, 1).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        assertThat(adapter(provider, docs, 0).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).isEmpty();
        assertThat(adapter(provider, docs, 2).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 1)).hasSize(2);
    }
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void thirtyThousandUtf8BytesFitButOneMoreFallsBack(String provider) {
        var first = notice(1, 5, "a", "가".repeat(4999));
        // Public applicability consumes the same budget as the full title/body; preserve the exact 30,000/30,001 boundary.
        int metadataBytes = (first.getSource().audience() + first.getSource().campus()).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        int remaining = 30_000 - 2 - 4999 * 3 - 2 * metadataBytes;
        String exactBody = "나".repeat(remaining / 3) + "x".repeat(remaining % 3);
        var exact = notice(2, 5, "b", exactBody);
        assertThat(adapter(provider, List.of(first, exact)).findCandidates(5L, NoticeLang.KO, "unmatchedsynonym", TODAY, 8)).hasSize(2);
        verifyNoInteractions(jdbc);
        var over = notice(2, 5, "b", exactBody + "x");
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
    @ParameterizedTest @ValueSource(strings = {"keyword", "postgres"})
    void reportsWhetherAnyEligibleNoticeExistsRegardlessOfCorpusSize(String provider) {
        var held = notice(1, 5, "기숙사", "신청"); held.holdSource("검토 필요");
        var foreign = notice(2, 99, "기숙사", "신청");
        assertThat(adapter(provider, List.of(held, foreign)).hasEligibleNotices(5L, NoticeLang.KO, TODAY)).isFalse();
        assertThat(adapter(provider, List.of(held, notice(3, 5, "보험", "가입"))).hasEligibleNotices(5L, NoticeLang.KO, TODAY)).isTrue();
        // Korean-only notice is not eligible for an English question.
        assertThat(adapter(provider, List.of(notice(3, 5, "보험", "가입"))).hasEligibleNotices(5L, NoticeLang.EN, TODAY)).isFalse();
        var large = LongStream.rangeClosed(1, 31).mapToObj(id -> notice(id, 5L, "기숙사", "신청 안내")).toList();
        assertThat(adapter(provider, large, 30).hasEligibleNotices(5L, NoticeLang.KO, TODAY)).isTrue();
        verifyNoInteractions(jdbc);
    }
}
