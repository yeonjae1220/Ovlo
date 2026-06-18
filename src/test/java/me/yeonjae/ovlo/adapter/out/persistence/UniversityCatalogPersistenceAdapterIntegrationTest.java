package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 통합 대학 카탈로그 검색을 실 PostgreSQL(Testcontainers) + Flyway로 검증.
 *
 * <p>검증 대상:
 * <ul>
 *   <li>리포트만/후기만/둘다 보유 대학이 모두 검색되고 플래그가 정확</li>
 *   <li>global 미연결 후기 대학(orphan)도 누락 없이 검색</li>
 *   <li>콘텐츠 없는 global 대학은 제외</li>
 *   <li>한글 키워드(name_ko)·현지어(local_name) 매칭, countryCode 필터</li>
 *   <li>후기 보유 대학의 reviewCount/avgRating 보강</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UniversityCatalogPersistenceAdapterIntegrationTest {

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

    @Autowired
    private SearchUniversityCatalogQuery catalogQuery;

    @Autowired
    private JdbcTemplate jdbc;

    private Long reportOnlyGlobalId;   // 리포트만
    private Long bothGlobalId;         // 리포트 + 후기
    private Long emptyGlobalId;        // 콘텐츠 없음 → 제외 대상
    private Long bothExchangeId;       // bothGlobal 의 후기 대학
    private Long orphanExchangeId;     // global 미연결 후기 대학

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM exchange_video_reviews");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM exchange_universities");
        jdbc.update("DELETE FROM global_universities");

        reportOnlyGlobalId = insertGlobal("Seoul National University", "서울대학교", "대한민국", "KR", "Seoul");
        bothGlobalId = insertGlobal("Technische Universitaet Muenchen", "뮌헨공과대학교", "독일", "DE", "Munich");
        emptyGlobalId = insertGlobal("Empty University", "빈대학교", "프랑스", "FR", "Paris");

        // 리포트만
        insertPublishedReport(reportOnlyGlobalId);
        // 둘다: 후기 대학 + 리포트
        bothExchangeId = insertExchange(bothGlobalId, "뮌헨공과대학교", "Technische Universitaet Muenchen", "독일", "DE", "Munich");
        insertPublishedReport(bothGlobalId);
        insertReview(bothExchangeId, "https://youtu.be/a", 4);
        insertReview(bothExchangeId, "https://youtu.be/b", 2);
        // orphan: global 미연결 후기 대학
        orphanExchangeId = insertExchange(null, "교토대학교", "Kyoto University", "일본", "JP", "Kyoto");
        insertReview(orphanExchangeId, "https://youtu.be/c", 5);
    }

    @Test
    @DisplayName("콘텐츠 보유 대학만 검색되고 콘텐츠 없는 대학은 제외된다")
    void returnsOnlyContentBearing() {
        List<UniversityCatalogResult> all = search(null, null);

        assertThat(all).extracting(UniversityCatalogResult::globalUnivId)
                .containsExactlyInAnyOrder(reportOnlyGlobalId, bothGlobalId, null);
        assertThat(all).noneMatch(r -> emptyGlobalId.equals(r.globalUnivId()));
    }

    @Test
    @DisplayName("리포트만 보유한 대학은 hasReport=true, hasReviews=false")
    void reportOnlyFlags() {
        UniversityCatalogResult r = find(reportOnlyGlobalId);
        assertThat(r.hasReport()).isTrue();
        assertThat(r.reportId()).isNotNull();
        assertThat(r.hasReviews()).isFalse();
        assertThat(r.reviewCount()).isZero();
    }

    @Test
    @DisplayName("리포트+후기 대학은 두 플래그가 모두 true이고 리뷰 집계가 채워진다")
    void bothFlagsAndAggregation() {
        UniversityCatalogResult r = find(bothGlobalId);
        assertThat(r.hasReport()).isTrue();
        assertThat(r.hasReviews()).isTrue();
        assertThat(r.exchangeUnivId()).isEqualTo(bothExchangeId);
        assertThat(r.reviewCount()).isEqualTo(2L);
        assertThat(r.avgRating()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("global 미연결 후기 대학(orphan)도 검색된다")
    void orphanExchangeIncluded() {
        List<UniversityCatalogResult> orphans = search("교토", null);
        assertThat(orphans).hasSize(1);
        UniversityCatalogResult r = orphans.get(0);
        assertThat(r.globalUnivId()).isNull();
        assertThat(r.exchangeUnivId()).isEqualTo(orphanExchangeId);
        assertThat(r.hasReviews()).isTrue();
        assertThat(r.reviewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("한글 이름(name_ko/local_name)으로 검색된다")
    void koreanKeywordMatches() {
        assertThat(search("서울", null)).hasSize(1);   // local_name=서울대학교
        assertThat(search("뮌헨", null)).hasSize(1);   // name_ko=뮌헨공과대학교
    }

    @Test
    @DisplayName("countryCode로 필터링된다")
    void countryFilter() {
        List<UniversityCatalogResult> jp = search(null, "JP");
        assertThat(jp).hasSize(1);
        assertThat(jp.get(0).exchangeUnivId()).isEqualTo(orphanExchangeId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<UniversityCatalogResult> search(String keyword, String countryCode) {
        PageResult<UniversityCatalogResult> page =
                catalogQuery.search(new SearchUniversityCatalogCommand(keyword, countryCode, 0, 50));
        return page.content();
    }

    private UniversityCatalogResult find(Long globalUnivId) {
        return search(null, null).stream()
                .filter(r -> globalUnivId.equals(r.globalUnivId()))
                .findFirst().orElseThrow();
    }

    private Long insertGlobal(String nameEn, String localName, String country, String code, String city) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, local_name, country, country_code, city) "
                        + "VALUES (?,?,?,?,?) RETURNING id",
                Long.class, nameEn, localName, country, code, city);
    }

    private Long insertExchange(Long globalId, String nameKo, String nameEn, String country, String code, String city) {
        return jdbc.queryForObject(
                "INSERT INTO exchange_universities (global_univ_id, name_ko, name_en, country, country_code, city) "
                        + "VALUES (?,?,?,?,?,?) RETURNING id",
                Long.class, globalId, nameKo, nameEn, country, code, city);
    }

    private void insertPublishedReport(Long globalId) {
        jdbc.update("INSERT INTO university_report (global_univ_id, status) VALUES (?, 'PUBLISHED')", globalId);
    }

    private void insertReview(Long exchangeId, String url, int rating) {
        jdbc.update(
                "INSERT INTO exchange_video_reviews (university_id, youtube_url, overall_rating) VALUES (?,?,?)",
                exchangeId, url, rating);
    }
}
