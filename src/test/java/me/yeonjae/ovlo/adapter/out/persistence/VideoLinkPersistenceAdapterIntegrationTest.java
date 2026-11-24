package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;
import me.yeonjae.ovlo.application.port.in.university.GetExchangeUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityVideoLinksQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore.DueLink;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 영상 링크 조회·최신성 갱신을 실 PostgreSQL(Testcontainers) + Flyway(V53 포함)로 검증한다.
 * 스케줄러는 꺼 두어 유튜브로 나가는 요청이 없다.
 */
@Tag("integration")
@SpringBootTest(properties = "ovlo.video-links.refresh-enabled=false")
@Testcontainers
@ActiveProfiles("test")
class VideoLinkPersistenceAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private VideoLinkStore store;
    @Autowired private GetUniversityVideoLinksQuery videoLinksQuery;
    @Autowired private GetExchangeUniversityQuery exchangeQuery;
    @Autowired private SearchUniversityCatalogQuery catalogQuery;
    @Autowired private JdbcTemplate jdbc;

    private Long wasedaId;
    private Long wasedaExchangeId;
    private Long hiddenExchangeId;
    private Long otherExchangeId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM exchange_video_reviews");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM exchange_universities");
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("DELETE FROM global_university_aliases");
        jdbc.update("DELETE FROM global_universities");

        wasedaId = insertGlobal("Waseda University", null);
        Long hiddenId = insertGlobal("Waseda Univ (old row)", wasedaId);   // 대표 행으로 숨긴 중복 행
        Long otherId = insertGlobal("Keio University", null);
        wasedaExchangeId = insertExchange(wasedaId, "Waseda University");
        hiddenExchangeId = insertExchange(hiddenId, "Waseda Univ (old row)");
        otherExchangeId = insertExchange(otherId, "Keio University");
    }

    @Test
    @DisplayName("대학의 영상 링크는 숨긴 중복 행의 것까지 모아 최신 게시순으로 주고, 다른 대학·숨긴 링크는 뺀다")
    void linksOfUniversity() {
        insertLink(wasedaExchangeId, "v-old", "2024-03-01T00:00:00Z", null);
        insertLink(wasedaExchangeId, "v-new", "2026-03-01T00:00:00Z", null);
        insertLink(hiddenExchangeId, "v-mid", "2025-03-01T00:00:00Z", null);
        insertLink(wasedaExchangeId, "v-gone", "2026-06-01T00:00:00Z", "2026-11-01T00:00:00Z");
        insertLink(otherExchangeId, "v-keio", "2026-09-01T00:00:00Z", null);

        var page = videoLinksQuery.getVideoLinks(wasedaId, 0, 2);

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.content()).extracting(VideoLinkResult::youtubeUrl).containsExactly(url("v-new"), url("v-mid"));
        assertThat(videoLinksQuery.getVideoLinks(wasedaId, 1, 2).content())
                .extracting(VideoLinkResult::youtubeUrl).containsExactly(url("v-old"));
        assertThat(page.content().get(0).title()).isEqualTo("title v-new");
        assertThat(page.content().get(0).channel()).isEqualTo("channel");
        assertThat(page.content().get(0).publishedAt().toInstant()).isEqualTo(Instant.parse("2026-03-01T00:00:00Z"));
    }

    @Test
    @DisplayName("숨긴 중복 행이나 비활성 대학의 id 로는 영상 링크를 주지 않는다 — 카탈로그와 같은 기준")
    void hiddenOrInactiveUniversityHasNoLinks() {
        insertLink(wasedaExchangeId, "v-rep", "2026-03-01T00:00:00Z", null);
        insertLink(hiddenExchangeId, "v-dup", "2025-03-01T00:00:00Z", null);
        Long hiddenId = jdbc.queryForObject("SELECT global_univ_id FROM exchange_universities WHERE id = ?", Long.class, hiddenExchangeId);

        assertThat(videoLinksQuery.getVideoLinks(wasedaId, 0, 10).totalElements()).isEqualTo(2);
        assertThat(videoLinksQuery.getVideoLinks(hiddenId, 0, 10).content()).isEmpty();
        assertThat(videoLinksQuery.getVideoLinks(hiddenId, 0, 10).totalElements()).isZero();
        assertThat(videoLinksQuery.getVideoLinks(987654321L, 0, 10).content()).isEmpty();

        jdbc.update("UPDATE global_universities SET inactive_reason = 'LICENSURE_REVOKED' WHERE id = ?", wasedaId);
        assertThat(videoLinksQuery.getVideoLinks(wasedaId, 0, 10).content()).isEmpty();
        assertThat(videoLinksQuery.getVideoLinks(wasedaId, 0, 10).totalElements()).isZero();
    }

    @Test
    @DisplayName("재확인 대기열은 확인한 적 없는 링크, 그다음 확인한 지 오래된 링크 순이고 숨긴 링크도 들어간다")
    void dueQueueOrder() {
        long recent = insertLink(wasedaExchangeId, "v-recent", "2026-01-01T00:00:00Z", null);
        long never = insertLink(wasedaExchangeId, "v-never", "2026-01-01T00:00:00Z", null);
        long oldHidden = insertLink(wasedaExchangeId, "v-old-hidden", "2026-01-01T00:00:00Z", "2026-10-01T00:00:00Z");
        setChecked(recent, "2026-11-20T00:00:00Z");
        setChecked(oldHidden, "2026-10-01T00:00:00Z");

        assertThat(store.findDueForCheck(10)).extracting(DueLink::id).containsExactly(never, oldHidden, recent);
        assertThat(store.findDueForCheck(1)).extracting(DueLink::youtubeUrl).containsExactly(url("v-never"));
    }

    @Test
    @DisplayName("있음 표시는 제목·채널명을 갱신하고 숨김을 풀며, 채널명이 없으면 기존 값을 둔다")
    void markAvailable() {
        long id = insertLink(wasedaExchangeId, "v1", "2026-01-01T00:00:00Z", "2026-10-01T00:00:00Z");
        Instant at = Instant.parse("2026-11-24T03:40:00Z");

        store.markAvailable(id, "새 제목", null, at);

        assertThat(row(id)).containsExactly("새 제목", "channel", Timestamp.from(at), null);
        assertThat(videoLinksQuery.getVideoLinks(wasedaId, 0, 10).content()).hasSize(1);
    }

    @Test
    @DisplayName("사라짐 표시는 처음 본 시각을 유지하고, 판정 보류는 확인 시각만 바꾼다")
    void markUnavailableAndChecked() {
        long id = insertLink(wasedaExchangeId, "v1", "2026-01-01T00:00:00Z", null);
        Instant first = Instant.parse("2026-11-24T03:40:00Z");
        Instant second = Instant.parse("2026-12-01T03:40:00Z");

        store.markUnavailable(id, first);
        store.markUnavailable(id, second);
        assertThat(row(id)).containsExactly("title v1", "channel", Timestamp.from(second), Timestamp.from(first));

        long other = insertLink(wasedaExchangeId, "v2", "2026-01-01T00:00:00Z", null);
        store.markChecked(other, first);
        assertThat(row(other)).containsExactly("title v2", "channel", Timestamp.from(first), null);
    }

    @Test
    @DisplayName("숨긴 링크는 기존 교환대학 후기 목록·건수와 카탈로그 후기 수에서도 빠진다")
    void hiddenLinksAreExcludedFromExistingReadPaths() {
        insertLink(wasedaExchangeId, "v-live", "2026-03-01T00:00:00Z", null);
        insertLink(wasedaExchangeId, "v-gone", "2026-06-01T00:00:00Z", "2026-11-01T00:00:00Z");

        var reviews = exchangeQuery.getReviews(new ExchangeUniversityId(wasedaExchangeId), null, 0, 10);
        assertThat(reviews.totalElements()).isEqualTo(1);
        assertThat(reviews.content()).extracting(r -> r.youtubeUrl()).containsExactly(url("v-live"));

        var catalog = catalogQuery.search(new SearchUniversityCatalogCommand("Waseda University", null, null, 0, 10));
        assertThat(catalog.content()).hasSize(1);
        assertThat(catalog.content().get(0).reviewCount()).isEqualTo(1L);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String url(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private Long insertGlobal(String nameEn, Long canonicalId) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, country, country_code, canonical_id) VALUES (?,?,?,?) RETURNING id",
                Long.class, nameEn, "일본", "JP", canonicalId);
    }

    private Long insertExchange(Long globalId, String nameEn) {
        return jdbc.queryForObject(
                "INSERT INTO exchange_universities (global_univ_id, name_en, country, country_code) VALUES (?,?,?,?) RETURNING id",
                Long.class, globalId, nameEn, "일본", "JP");
    }

    private long insertLink(Long exchangeId, String videoId, String publishedAt, String unavailableSince) {
        return jdbc.queryForObject(
                "INSERT INTO exchange_video_reviews (university_id, youtube_url, title, channel, published_at, source, unavailable_since) "
                        + "VALUES (?,?,?,?,?::timestamptz,?,?::timestamptz) RETURNING id",
                Long.class, exchangeId, url(videoId), "title " + videoId, "channel", publishedAt, "channel:test", unavailableSince);
    }

    private void setChecked(long id, String at) {
        jdbc.update("UPDATE exchange_video_reviews SET last_checked_at = ?::timestamptz WHERE id = ?", at, id);
    }

    private List<Object> row(long id) {
        return jdbc.queryForObject(
                "SELECT title, channel, last_checked_at, unavailable_since FROM exchange_video_reviews WHERE id = ?",
                (rs, n) -> java.util.Arrays.asList(rs.getString(1), rs.getString(2), rs.getTimestamp(3), rs.getTimestamp(4)), id);
    }
}
