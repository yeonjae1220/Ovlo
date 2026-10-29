package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;
import me.yeonjae.ovlo.application.dto.result.UniversityReportSummaryResult;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityReportPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.UniversityDomainLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 중복 대학 행을 canonical_id 로 숨기는 동작을 실 PostgreSQL(Testcontainers) + Flyway(V32)로 검증.
 *
 * <p>같은 학교가 카탈로그에 두 줄로 있으면(옛 이름·학생 메일 도메인 행 등) 패배 행에
 * canonical_id 를 채운다. 패배 행은 지우지 않는다 — 그 행의 domain 이 학교 이메일 인증에
 * 쓰이기 때문이다. 검증 대상:
 * <ul>
 *   <li>본교 검색·통합 카탈로그·리포트 목록에서 패배 행이 빠진다</li>
 *   <li>패배 행 도메인으로 이메일 역조회하면 대표 행이 나온다</li>
 *   <li>패배 행의 옛 이름으로 검색해도 대표 행이 나온다</li>
 *   <li>교환대학 목록 검색·건수·국가 집계에서도 숨김 행에 붙은 교환대학이 빠진다</li>
 *   <li>자기 자신을 가리키는 canonical_id 는 거부된다</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UniversityCanonicalDedupIntegrationTest {

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
    private SearchUniversityPort searchPort;

    @Autowired
    private UniversityDomainLookupPort domainLookupPort;

    @Autowired
    private SearchUniversityCatalogQuery catalogQuery;

    @Autowired
    private LoadUniversityReportPort reportPort;

    @Autowired
    private LoadExchangeUniversityPort exchangePort;

    @Autowired
    private JdbcTemplate jdbc;

    private Long canonicalId;   // KU Leuven (대표)
    private Long hiddenId;      // Katholieke Universiteit Leuven (숨김, 학생 메일 도메인 보유)
    private Long canonicalReportId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM university_report_translation");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM exchange_universities");
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("UPDATE global_universities SET canonical_id = NULL");
        jdbc.update("DELETE FROM global_universities");

        canonicalId = insertGlobal("KU Leuven", "kuleuven.be");
        hiddenId = insertGlobal("Katholieke Universiteit Leuven", "student.kuleuven.be");
        jdbc.update("UPDATE global_universities SET canonical_id = ? WHERE id = ?", canonicalId, hiddenId);

        insertExchange(canonicalId, "KU Leuven");
        insertExchange(hiddenId, "Katholieke Universiteit Leuven");
        canonicalReportId = insertPublishedReport(canonicalId);
        insertPublishedReport(hiddenId);
    }

    @Test
    @DisplayName("본교 검색에서 숨김 행이 빠지고 건수도 대표 행만 센다")
    void universitySearchExcludesHidden() {
        assertThat(searchPort.search("Leuven", null, 0, 20))
                .extracting(u -> u.getId().value())
                .containsExactly(canonicalId);
        assertThat(searchPort.count("Leuven", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("숨김 행의 도메인으로 이메일 역조회하면 대표 행이 나온다")
    void emailDomainResolvesToCanonical() {
        assertThat(domainLookupPort.findByEmailDomain("student.kuleuven.be"))
                .extracting(u -> u.getId().value())
                .containsExactly(canonicalId);
        assertThat(domainLookupPort.findByEmailDomain("kuleuven.be"))
                .extracting(u -> u.getId().value())
                .containsExactly(canonicalId);
    }

    @Test
    @DisplayName("통합 카탈로그에서 숨김 행이 빠진다")
    void catalogExcludesHidden() {
        var page = catalogQuery.search(new SearchUniversityCatalogCommand("Leuven", null, 0, 20));
        assertThat(page.content())
                .extracting(UniversityCatalogResult::globalUnivId)
                .containsExactly(canonicalId);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("국가별 집계도 숨김 행을 세지 않는다")
    void countryCountExcludesHidden() {
        assertThat(catalogQuery.getCountries())
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.countryCode()).isEqualTo("BE");
                    assertThat(c.universityCount()).isEqualTo(1L);
                });
    }

    @Test
    @DisplayName("숨김 행과 대표 행이 같은 도메인이어도 대표 행 하나만 나온다")
    void sharedDomainYieldsSingleCanonical() {
        jdbc.update("UPDATE global_universities SET domain = 'kuleuven.be' WHERE id = ?", hiddenId);
        assertThat(domainLookupPort.findByEmailDomain("kuleuven.be"))
                .extracting(u -> u.getId().value())
                .containsExactly(canonicalId);
    }

    @Test
    @DisplayName("리포트 목록 키워드 검색에서도 숨김 행의 리포트가 빠진다")
    void reportKeywordSearchExcludesHidden() {
        var page = reportPort.findPageByLang("ko", "Leuven", 0, 20);
        assertThat(page.content())
                .extracting(UniversityReportSummaryResult::id)
                .containsExactly(canonicalReportId);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("리포트 목록에서 숨김 행의 리포트가 빠지고, 대학 미연결 리포트는 남는다")
    void reportListExcludesHidden() {
        Long unlinkedReportId = insertPublishedReport(null);

        var page = reportPort.findPageByLang("ko", null, 0, 20);
        assertThat(page.content())
                .extracting(UniversityReportSummaryResult::id)
                .containsExactlyInAnyOrder(canonicalReportId, unlinkedReportId);
        assertThat(page.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("숨긴 행의 옛 이름으로 본교 검색하면 대표 행이 나온다")
    void universitySearchByHiddenNameFindsCanonical() {
        assertThat(searchPort.search("Katholieke", null, 0, 20))
                .extracting(u -> u.getId().value())
                .containsExactly(canonicalId);
        assertThat(searchPort.count("Katholieke", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("숨긴 행의 옛 이름·다국어 이름으로 카탈로그 검색하면 대표 행이 나온다")
    void catalogSearchByHiddenNameFindsCanonical() {
        jdbc.update("INSERT INTO global_university_names (global_univ_id, lang, name) VALUES (?, 'nl', ?)",
                hiddenId, "Katholieke Hogeschool Leuven-oud");

        for (String keyword : new String[] {"Katholieke", "Hogeschool Leuven-oud"}) {
            var page = catalogQuery.search(new SearchUniversityCatalogCommand(keyword, null, 0, 20));
            assertThat(page.content())
                    .as(keyword)
                    .extracting(UniversityCatalogResult::globalUnivId)
                    .containsExactly(canonicalId);
            assertThat(page.totalElements()).as(keyword).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("숨긴 행의 옛 이름으로 리포트 목록을 검색하면 대표 행의 리포트가 나온다")
    void reportSearchByHiddenNameFindsCanonical() {
        var page = reportPort.findPageByLang("ko", "Katholieke", 0, 20);
        assertThat(page.content())
                .extracting(UniversityReportSummaryResult::id)
                .containsExactly(canonicalReportId);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("교환대학 목록 검색·건수에서 숨김 행에 붙은 교환대학이 빠지고, 대학 미연결 교환대학은 남는다")
    void exchangeUniversitySearchExcludesHidden() {
        insertExchange(null, "Leuven Summer School");

        assertThat(exchangePort.search("Leuven", null, 0, 20))
                .extracting(u -> u.getNameEn())
                .containsExactlyInAnyOrder("KU Leuven", "Leuven Summer School");
        assertThat(exchangePort.count("Leuven", null)).isEqualTo(2L);
    }

    @Test
    @DisplayName("교환대학 국가별 집계도 숨김 행에 붙은 교환대학을 세지 않는다")
    void exchangeCountryCountExcludesHidden() {
        jdbc.update("UPDATE exchange_universities SET country = '벨기에'");

        assertThat(exchangePort.findCountries())
                .singleElement()
                .satisfies(c -> assertThat(c.universityCount()).isEqualTo(1L));
    }

    @Test
    @DisplayName("자기 자신을 가리키는 canonical_id 는 거부된다")
    void selfReferenceRejected() {
        assertThatThrownBy(() ->
                jdbc.update("UPDATE global_universities SET canonical_id = id WHERE id = ?", canonicalId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long insertGlobal(String nameEn, String domain) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, country, country_code, domain) "
                        + "VALUES (?, '벨기에', 'BE', ?) RETURNING id",
                Long.class, nameEn, domain);
    }

    private void insertExchange(Long globalId, String nameEn) {
        jdbc.update("INSERT INTO exchange_universities (global_univ_id, name_en, country_code) VALUES (?,?, 'BE')",
                globalId, nameEn);
    }

    private Long insertPublishedReport(Long globalId) {
        return jdbc.queryForObject(
                "INSERT INTO university_report (global_univ_id, status) VALUES (?, 'PUBLISHED') RETURNING id",
                Long.class, globalId);
    }
}
