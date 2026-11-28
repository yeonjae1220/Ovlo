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
 *   <li>후기 보유 대학의 reviewCount 보강</li>
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
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("DELETE FROM global_university_aliases");
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

    @Test void exchangeMasterAloneDoesNotMakeContentAndCountriesHaveUniqueCodes() {
        insertExchange(emptyGlobalId, "빈대학교", "Empty University", "France", "FR", "Paris");
        insertExchange(null, "빈 후기", "No content", "Japan", "JP", "Tokyo");
        Long incorrect = insertGlobal("Korea University", "고려대학교", "일본", "KR", "Seoul");
        insertPublishedReport(incorrect);
        assertThat(search(null, null)).noneMatch(row -> emptyGlobalId.equals(row.globalUnivId()));
        var korea = catalogQuery.getCountries().stream().filter(c -> "KR".equals(c.countryCode())).toList();
        assertThat(korea).hasSize(1);
        assertThat(korea.getFirst().universityCount()).isEqualTo(2);
        assertThat(korea.getFirst().country()).isNotEqualTo("일본");
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

    @Test void countryRepairTargetsOnlyTheVerifiedKoreanUniversity() {
        jdbc.update("INSERT INTO global_universities(id,name_en,country,country_code) VALUES(5056,'Korea University','일본','KR')");
        Long japanese = insertGlobal("Korea University", "조선대학교", "일본", "JP", "Tokyo");
        Long koreanExchange = insertExchange(5056L, "고려대학교", "Korea University", "Japan", "KR", "Seoul");
        Long japaneseExchange = insertExchange(japanese, "조선대학교", "Korea University (Japan)", "Japan", "JP", "Tokyo");
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V55__korea_university_country_label.sql"));
        script.execute(jdbc.getDataSource());
        script.execute(jdbc.getDataSource()); // The repair is safe to verify repeatedly.
        assertThat(jdbc.queryForObject("SELECT country FROM global_universities WHERE id=5056", String.class)).isEqualTo("대한민국");
        assertThat(jdbc.queryForObject("SELECT country FROM exchange_universities WHERE id=?", String.class, koreanExchange)).isEqualTo("대한민국");
        assertThat(jdbc.queryForObject("SELECT country FROM global_universities WHERE id=?", String.class, japanese)).isEqualTo("일본");
        assertThat(jdbc.queryForObject("SELECT country FROM exchange_universities WHERE id=?", String.class, japaneseExchange)).isEqualTo("Japan");
    }

    @Test void englishCountryRepairTargetsOnlyTheVerifiedKoreanUniversity() {
        jdbc.update("INSERT INTO global_universities(id,name_en,country,country_en,country_code) "
                + "VALUES(5056,'Korea University','대한민국','Japan','KR')");
        Long japanese = insertGlobal("Korea University (Japan)", "조선대학교", "일본", "JP", "Tokyo");
        jdbc.update("UPDATE global_universities SET country_en='Japan' WHERE id=?", japanese);
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V60__korea_university_country_en.sql"));
        script.execute(jdbc.getDataSource());
        script.execute(jdbc.getDataSource()); // The repair is safe to verify repeatedly.
        assertThat(jdbc.queryForObject("SELECT country_en FROM global_universities WHERE id=5056", String.class))
                .isEqualTo("Korea, Republic of");
        assertThat(jdbc.queryForObject("SELECT country FROM global_universities WHERE id=5056", String.class))
                .isEqualTo("대한민국");
        assertThat(jdbc.queryForObject("SELECT country_en FROM global_universities WHERE id=?", String.class, japanese))
                .isEqualTo("Japan");
    }

    @Test void productionVerificationFixtureIsHiddenWithoutTouchingRealUniversities() {
        jdbc.update("INSERT INTO global_universities(id,name_en,country,country_en,country_code,domain) VALUES"
                + "(20307,'[TEST ONLY] Ovlo Production Verification 20261112','대한민국','South Korea','KR','ovlo-test.invalid')");
        Long real = insertGlobal("Seoul National University", "서울대학교", "대한민국", "KR", "Seoul");
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.ClassPathResource("db/migration/V61__hide_production_verification_university.sql"));
        script.execute(jdbc.getDataSource());
        script.execute(jdbc.getDataSource()); // The repair is safe to verify repeatedly.
        assertThat(jdbc.queryForObject("SELECT inactive_reason FROM global_universities WHERE id=20307", String.class))
                .isEqualTo("TEST_FIXTURE");
        assertThat(jdbc.queryForObject("SELECT inactive_reason FROM global_universities WHERE id=?", String.class, real))
                .isNull();
    }

    @Test void unavailableVideosDoNotCountAsPublishedContent() {
        Long exchange = insertExchange(emptyGlobalId, "숨겨진 영상", "Hidden video university", "France", "FR", "Paris");
        insertReview(exchange, "https://youtu.be/hidden", 0);
        jdbc.update("UPDATE exchange_video_reviews SET unavailable_since=now() WHERE university_id IN (?,?)", exchange, orphanExchangeId);
        assertThat(search(null, null)).noneMatch(row -> emptyGlobalId.equals(row.globalUnivId())
                || orphanExchangeId.equals(row.exchangeUnivId()));
    }

    @Test
    @DisplayName("리포트+후기 대학은 두 플래그가 모두 true이고 리뷰 집계가 채워진다")
    void bothFlagsAndAggregation() {
        UniversityCatalogResult r = find(bothGlobalId);
        assertThat(r.hasReport()).isTrue();
        assertThat(r.hasReviews()).isTrue();
        assertThat(r.exchangeUnivId()).isEqualTo(bothExchangeId);
        assertThat(r.reviewCount()).isEqualTo(2L);
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
    @DisplayName("다국어 이름(global_university_names)으로 검색된다 — ja/zh 라벨 매칭")
    void multilingualNameMatches() {
        // bothGlobal(뮌헨공대)에 일본어·중국어 라벨 시드
        insertGlobalName(bothGlobalId, "ja", "ミュンヘン工科大学");
        insertGlobalName(bothGlobalId, "zh", "慕尼黑工业大学");

        // 일본어 라벨로 검색 → 매칭 (name_en/name_ko 로는 안 잡히는 키워드)
        assertThat(search("ミュンヘン工科大学", null))
                .extracting(UniversityCatalogResult::globalUnivId).containsExactly(bothGlobalId);
        // 중국어 부분 문자열로 검색 → 매칭
        assertThat(search("慕尼黑", null))
                .extracting(UniversityCatalogResult::globalUnivId).containsExactly(bothGlobalId);
        // 다른 대학의 다국어 라벨과 혼동되지 않음
        assertThat(search("ミュンヘン工科大学", null)).hasSize(1);
    }

    @Test
    @DisplayName("표시 언어(lang)에 따라 nameLocal 이 그 언어 이름으로 채워지고, 없으면 null 로 영문 폴백을 맡긴다")
    void nameLocalFollowsDisplayLang() {
        insertGlobalName(bothGlobalId, "ja", "ミュンヘン工科大学");

        // ko: 통합 한글명(name_ko) — exchange.name_ko 우선, 없으면 local_name
        assertThat(find(bothGlobalId, "ko").nameLocal()).isEqualTo("뮌헨공과대학교");
        assertThat(find(reportOnlyGlobalId, "ko").nameLocal()).isEqualTo("서울대학교");
        // en: 영문명 그대로
        assertThat(find(bothGlobalId, "en").nameLocal()).isEqualTo("Technische Universitaet Muenchen");
        // ja: 다국어 이름 테이블의 라벨, 라벨이 없는 대학은 null
        assertThat(find(bothGlobalId, "ja").nameLocal()).isEqualTo("ミュンヘン工科大学");
        assertThat(find(reportOnlyGlobalId, "ja").nameLocal()).isNull();
        // lang 생략 → 영문
        assertThat(find(bothGlobalId, null).nameLocal()).isEqualTo("Technische Universitaet Muenchen");
        // orphan(글로벌 미연결) 행도 ko/en 은 채워지고 그 외 언어는 null
        UniversityCatalogResult orphanJa = search("교토", null, "ja").get(0);
        assertThat(orphanJa.nameLocal()).isNull();
        assertThat(search("교토", null, "ko").get(0).nameLocal()).isEqualTo("교토대학교");
    }

    @Test
    @DisplayName("별칭(약어·옛 이름)으로 카탈로그를 검색한다")
    void aliasMatches() {
        jdbc.update("INSERT INTO global_university_aliases (global_univ_id, alias) VALUES (?, ?)", bothGlobalId, "뮌헨공대");

        assertThat(search("뮌헨공대", null))
                .extracting(UniversityCatalogResult::globalUnivId).containsExactly(bothGlobalId);
        assertThat(catalogQuery.search(new SearchUniversityCatalogCommand("뮌헨공대", null, null, 0, 50)).totalElements())
                .isEqualTo(1L);
        assertThat(search("없는별칭", null)).isEmpty();
    }

    @Test
    @DisplayName("교환대학 행이 여러 개면 한글명이 있는 행을 고른다 — 삽입 순서와 무관")
    void picksExchangeRowWithKoreanName() {
        Long globalId = insertGlobal("University of Sao Paulo", null, "브라질", "BR", null);
        insertPublishedReport(globalId); // The name-ordering case still needs actual catalog content.
        insertExchange(globalId, "  ", "Universidade de Sao Paulo", "브라질", "BR", null);        // 마스터 유래 행(한글명이 공백뿐)
        Long curatedId = insertExchange(globalId, "상파울루대학교", "USP", "브라질", "BR", "상파울루");  // 시드 행
        insertExchange(globalId, null, "USP (duplicate)", "브라질", "BR", null);

        UniversityCatalogResult r = find(globalId, "ko");

        assertThat(r.exchangeUnivId()).isEqualTo(curatedId);
        assertThat(r.nameKo()).isEqualTo("상파울루대학교");
        assertThat(r.city()).isEqualTo("상파울루");
    }

    @Test
    @DisplayName("교환대학 행이 여러 개면 후기가 달린 행을 한글명 행보다 먼저 고른다 — 후기 링크·집계가 그 행 기준")
    void picksExchangeRowWithReviewsFirst() {
        Long globalId = insertGlobal("University College London", null, "영국", "GB", null);
        insertExchange(globalId, "유니버시티 칼리지 런던", "UCL", "영국", "GB", "런던");
        Long reviewedId = insertExchange(globalId, null, "University College London, University of London", "영국", "GB", null);
        insertReview(reviewedId, "https://youtu.be/ucl", 5);

        UniversityCatalogResult r = find(globalId);

        assertThat(r.exchangeUnivId()).isEqualTo(reviewedId);
        assertThat(r.reviewCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("조건이 같은 교환대학 행이 여러 개면 id 가 가장 작은 행을 고른다 — 페이지마다 같은 행")
    void picksLowestIdOnTie() {
        Long globalId = insertGlobal("Tie University", null, "독일", "DE", null);
        insertPublishedReport(globalId);
        Long firstId = insertExchange(globalId, "동률대학교 A", "Tie University A", "독일", "DE", null);
        insertExchange(globalId, "동률대학교 B", "Tie University B", "독일", "DE", null);
        // 첫 행을 갱신해 물리적 위치를 뒤로 보낸다 — 정렬이 없으면 두 번째 행이 먼저 읽힌다
        jdbc.update("UPDATE exchange_universities SET city = 'Berlin' WHERE id = ?", firstId);

        assertThat(find(globalId).exchangeUnivId()).isEqualTo(firstId);
    }

    @Test
    @DisplayName("countryCode로 필터링된다")
    void countryFilter() {
        List<UniversityCatalogResult> jp = search(null, "JP");
        assertThat(jp).hasSize(1);
        assertThat(jp.get(0).exchangeUnivId()).isEqualTo(orphanExchangeId);
    }

    @Test
    @DisplayName("국가 목록은 리포트만 있는 국가도 포함하고 콘텐츠 없는 국가는 제외한다")
    void countryListIncludesReportOnly() {
        var countries = catalogQuery.getCountries();

        assertThat(countries).extracting(c -> c.countryCode())
                .containsExactlyInAnyOrder("KR", "DE", "JP");  // KR=리포트만 포함, FR(빈대학) 제외
        assertThat(countries).allSatisfy(c -> assertThat(c.universityCount()).isEqualTo(1L));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<UniversityCatalogResult> search(String keyword, String countryCode) {
        return search(keyword, countryCode, null);
    }

    private List<UniversityCatalogResult> search(String keyword, String countryCode, String lang) {
        PageResult<UniversityCatalogResult> page =
                catalogQuery.search(new SearchUniversityCatalogCommand(keyword, countryCode, lang, 0, 50));
        return page.content();
    }

    private UniversityCatalogResult find(Long globalUnivId) {
        return find(globalUnivId, null);
    }

    private UniversityCatalogResult find(Long globalUnivId, String lang) {
        return search(null, null, lang).stream()
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

    private void insertGlobalName(Long globalId, String lang, String name) {
        jdbc.update("INSERT INTO global_university_names (global_univ_id, lang, name) VALUES (?,?,?)",
                globalId, lang, name);
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
