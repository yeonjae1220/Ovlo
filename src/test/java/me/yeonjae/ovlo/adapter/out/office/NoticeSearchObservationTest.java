package me.yeonjae.ovlo.adapter.out.office;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.application.service.office.NoticeGovernanceService;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.stream.LongStream;
import static me.yeonjae.ovlo.adapter.out.office.SmallNoticeCorpusSearchTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticeSearchObservationTest {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final OfficeMetrics metrics = new OfficeMetrics(registry);
    final LoadNoticePort load = mock(LoadNoticePort.class);
    final NoticeGovernanceService governance = mock(NoticeGovernanceService.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    NoticeSearchPort adapter(String provider, List<Notice> notices) {
        when(load.findActiveForStudent(5L, null, TODAY)).thenReturn(notices);
        lenient().when(governance.eligible(any(), eq(TODAY))).thenReturn(true);
        return provider.equals("KEYWORD") ? new KeywordNoticeSearchAdapter(load, governance, metrics)
                : new PostgresFtsNoticeSearchAdapter(jdbc, load, governance, metrics);
    }
    List<Notice> largeCorpus() {
        return LongStream.rangeClosed(1, 9).mapToObj(id -> notice(id, 5, "기숙사 신청", "등록 안내")).toList();
    }
    void onlyOne(String provider, String path, String candidates) {
        assertThat(registry.get("ovlo.office.search.completed").tags("provider", provider, "path", path,
                "candidates", candidates).counter().count()).isEqualTo(1);
        assertThat(total()).isEqualTo(1);
    }
    double total() {
        return registry.find("ovlo.office.search.completed").counters().stream().mapToDouble(c -> c.count()).sum();
    }

    @ParameterizedTest @ValueSource(strings = {"KEYWORD", "POSTGRES"})
    void noEligibleIsDistinctFromRankedMiss(String provider) {
        var held = notice(1, 5, "기숙사", "안내"); held.holdSource("hold");
        var otherOffice = notice(2, 99, "기숙사", "안내");
        assertThat(adapter(provider, List.of(held, otherOffice)).findCandidates(5L, NoticeLang.KO, "question", TODAY, 8)).isEmpty();
        onlyOne(provider, "NO_ELIGIBLE", "EMPTY");
    }
    @ParameterizedTest @ValueSource(strings = {"KEYWORD", "POSTGRES"})
    void smallCorpusDoesNotRequireLexicalMatch(String provider) {
        assertThat(adapter(provider, List.of(notice(1, 5, "기숙사", "안내")))
                .findCandidates(5L, NoticeLang.KO, "unmatched", TODAY, 8)).hasSize(1);
        onlyOne(provider, "SMALL_CORPUS", "PRESENT");
    }
    @ParameterizedTest @ValueSource(strings = {"KEYWORD", "POSTGRES"})
    void rankedMissAndEmptyQueryAreNotMissingCorpus(String provider) {
        var search = adapter(provider, largeCorpus());
        assertThat(search.findCandidates(5L, NoticeLang.KO, "?", TODAY, 8)).isEmpty();
        onlyOne(provider, "RANKED", "EMPTY");
    }
    @ParameterizedTest @ValueSource(strings = {"KEYWORD", "POSTGRES"})
    void byteBudgetOverflowAlsoUsesRankedPath(String provider) {
        assertThat(adapter(provider, List.of(notice(1, 5, "a", "가".repeat(10000))))
                .findCandidates(5L, NoticeLang.KO, "unmatched", TODAY, 8)).isEmpty();
        onlyOne(provider, "RANKED", "EMPTY");
    }
    @ParameterizedTest @ValueSource(strings = {"KEYWORD", "POSTGRES"})
    void cjkFallbackCountsOnlyTheOuterProviderOnce(String provider) {
        assertThat(adapter(provider, largeCorpus()).findCandidates(5L, NoticeLang.KO, "기숙사 신청", TODAY, 8)).hasSize(8);
        onlyOne(provider, "RANKED", "PRESENT");
    }
    @Test void databaseErrorIsNotReportedAsAnEmptyCompletedSearch() {
        var search = adapter("POSTGRES", largeCorpus());
        var failure = new IllegalStateException("synthetic failure");
        when(jdbc.queryForList(anyString(), eq(Long.class), any(Object[].class))).thenThrow(failure);
        assertThatThrownBy(() -> search.findCandidates(5L, NoticeLang.KO, "dormitory", TODAY, 8)).isSameAs(failure);
        assertThat(total()).isZero();
    }
}
