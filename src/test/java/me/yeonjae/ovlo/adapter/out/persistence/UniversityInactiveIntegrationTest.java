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
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 폐교·인가취소 대학 행을 inactive_reason 으로 숨기는 동작을 실 PostgreSQL(Testcontainers) + Flyway(V42)로 검증.
 *
 * <p>canonical_id 와 달리 이어 줄 대표 행이 없다. 검증 대상:
 * <ul>
 *   <li>본교 검색·통합 카탈로그·국가 집계·리포트 목록·교환대학 목록에서 비활성 행이 빠진다</li>
 *   <li>비활성 행의 도메인으로 이메일 역조회는 그대로 된다(졸업생 인증)</li>
 *   <li>비활성 행을 대표로 가리키는 중복 행의 이름으로 찾아도 나오지 않는다</li>
 *   <li>V42 의 데이터 UPDATE 가 의도한 두 행만 표시한다(테스트 DB 는 Flyway 시점에 비어 있어 직접 다시 돌린다)</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UniversityInactiveIntegrationTest {

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

    private Long activeId;        // Dubai Active University
    private Long inactiveId;      // Dubai Revoked College (인가 취소)
    private Long activeReportId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM university_report_translation");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM exchange_universities");
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("DELETE FROM global_university_aliases");
        jdbc.update("UPDATE global_universities SET canonical_id = NULL");
        jdbc.update("DELETE FROM global_universities");

        activeId = insertGlobal("Dubai Active University", "active.ac.ae");
        inactiveId = insertGlobal("Dubai Revoked College", "revoked.ac.ae");
        jdbc.update("UPDATE global_universities SET inactive_reason = 'LICENSURE_REVOKED' WHERE id = ?", inactiveId);

        insertExchange(activeId, "Dubai Active University");
        insertExchange(inactiveId, "Dubai Revoked College");
        activeReportId = insertPublishedReport(activeId);
        insertPublishedReport(inactiveId);
    }

    @Test
    @DisplayName("본교 검색에서 비활성 행이 빠지고 건수도 활성 행만 센다")
    void universitySearchExcludesInactive() {
        assertThat(searchPort.search("Dubai", null, 0, 20))
                .extracting(u -> u.getId().value())
                .containsExactly(activeId);
        assertThat(searchPort.count("Dubai", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("비활성 행의 도메인으로 이메일 역조회는 그대로 된다")
    void emailDomainStillResolvesInactive() {
        assertThat(domainLookupPort.findByEmailDomain("revoked.ac.ae"))
                .extracting(u -> u.getId().value())
                .containsExactly(inactiveId);
    }

    @Test
    @DisplayName("통합 카탈로그와 국가별 집계에서 비활성 행이 빠진다")
    void catalogExcludesInactive() {
        var page = catalogQuery.search(new SearchUniversityCatalogCommand("Dubai", null, null, 0, 20));
        assertThat(page.content())
                .extracting(UniversityCatalogResult::globalUnivId)
                .containsExactly(activeId);
        assertThat(page.totalElements()).isEqualTo(1L);
        assertThat(catalogQuery.getCountries())
                .singleElement()
                .satisfies(c -> assertThat(c.universityCount()).isEqualTo(1L));
    }

    @Test
    @DisplayName("리포트 목록에서 비활성 행의 리포트가 빠진다")
    void reportListExcludesInactive() {
        var page = reportPort.findPageByLang("ko", null, 0, 20);
        assertThat(page.content())
                .extracting(UniversityReportSummaryResult::id)
                .containsExactly(activeReportId);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("교환대학 목록 검색·건수·국가 집계에서 비활성 행에 붙은 교환대학이 빠진다")
    void exchangeUniversitySearchExcludesInactive() {
        jdbc.update("UPDATE exchange_universities SET country = '아랍에미리트'");

        assertThat(exchangePort.search("Dubai", null, 0, 20))
                .extracting(u -> u.getNameEn())
                .containsExactly("Dubai Active University");
        assertThat(exchangePort.count("Dubai", null)).isEqualTo(1L);
        assertThat(exchangePort.findCountries())
                .singleElement()
                .satisfies(c -> assertThat(c.universityCount()).isEqualTo(1L));
    }

    @Test
    @DisplayName("비활성 행의 별칭·다국어 이름·교환대학 한글명으로 찾아도 나오지 않는다")
    void keywordBranchesExcludeInactive() {
        jdbc.update("INSERT INTO global_university_aliases (global_univ_id, alias) VALUES (?, ?)", inactiveId, "리보크대");
        jdbc.update("INSERT INTO global_university_names (global_univ_id, lang, name) VALUES (?, 'ja', ?)",
                inactiveId, "ドバイ取消大学");
        jdbc.update("UPDATE exchange_universities SET name_ko = '두바이취소대학' WHERE global_univ_id = ?", inactiveId);

        for (String keyword : new String[] {"리보크대", "ドバイ取消大学", "두바이취소대학"}) {
            assertThat(searchPort.count(keyword, null)).as(keyword).isZero();
            assertThat(catalogQuery.search(new SearchUniversityCatalogCommand(keyword, "AE", null, 0, 20)).totalElements())
                    .as(keyword).isZero();
        }
    }

    @Test
    @DisplayName("비활성 행을 대표로 가리키는 중복 행은 그 옛 이름으로 찾아도 나오지 않는다")
    void duplicateOfInactiveStaysHidden() {
        Long duplicateId = insertGlobal("Revoked College of Dubai (old name)", "old-revoked.ac.ae");
        jdbc.update("UPDATE global_universities SET canonical_id = ? WHERE id = ?", inactiveId, duplicateId);

        assertThat(searchPort.count("old name", null)).isZero();
        assertThat(catalogQuery.search(new SearchUniversityCatalogCommand("old name", null, null, 0, 20)).totalElements())
                .isZero();
        assertThat(reportPort.findPageByLang("ko", "old name", 0, 20).totalElements()).isZero();
    }

    @Test
    @DisplayName("inactive_reason 을 비우면(되돌리기) 다시 검색·카탈로그·리포트 목록에 나온다")
    void reactivatedRowReappears() {
        jdbc.update("UPDATE global_universities SET inactive_reason = NULL WHERE id = ?", inactiveId);

        assertThat(searchPort.count("Dubai", "AE")).isEqualTo(2L);
        assertThat(catalogQuery.search(new SearchUniversityCatalogCommand("Dubai", "AE", null, 0, 20)).totalElements())
                .isEqualTo(2L);
        assertThat(reportPort.findPageByLang("ko", null, 0, 20).totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("V42 데이터 UPDATE 는 도메인(대소문자 무시)·영문명·국가가 맞고 중복이 아닌 행만 표시한다")
    void migrationUpdateMarksOnlyIntendedRows() throws IOException {
        // name_en 은 UNIQUE 라 같은 이름의 미끼를 둘 수 없다 — 조건마다 한 가지씩만 어긋난 행으로 본다.
        Long ittihad = insertGlobal("Ittihad University", "ITTIHAD.ac.ae");           // 대상(도메인 대소문자만 다름)
        Long acdDuplicate = insertGlobal("American College Of Dubai", "acd.ac.ae");    // 이미 중복 행 → 제외
        jdbc.update("UPDATE global_universities SET canonical_id = ? WHERE id = ?", activeId, acdDuplicate);
        Long wrongName = insertGlobal("Ittihad University College", "ittihad.ac.ae"); // 이름 불일치 → 제외

        String script = new ClassPathResource("db/migration/V42__add_inactive_reason_to_global_universities.sql")
                .getContentAsString(StandardCharsets.UTF_8);
        String update = script.substring(script.indexOf("UPDATE global_universities"));
        assertThat(jdbc.update(update)).isEqualTo(1);
        assertThat(jdbc.update(update)).isZero();   // 재실행해도 더 바꾸지 않는다

        assertThat(reasonOf(ittihad)).isEqualTo("LICENSURE_REVOKED");
        assertThat(reasonOf(acdDuplicate)).isNull();
        assertThat(reasonOf(wrongName)).isNull();

        jdbc.update("UPDATE global_universities SET country_code = 'SY', inactive_reason = NULL WHERE id = ?", ittihad);
        assertThat(jdbc.update(update)).as("국가가 다르면 제외").isZero();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long insertGlobal(String nameEn, String domain) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, country, country_code, domain) "
                        + "VALUES (?, '아랍에미리트', 'AE', ?) RETURNING id",
                Long.class, nameEn, domain);
    }

    private String reasonOf(Long id) {
        return jdbc.queryForObject("SELECT inactive_reason FROM global_universities WHERE id = ?", String.class, id);
    }

    private void insertExchange(Long globalId, String nameEn) {
        jdbc.update("INSERT INTO exchange_universities (global_univ_id, name_en, country_code) VALUES (?,?, 'AE')",
                globalId, nameEn);
    }

    private Long insertPublishedReport(Long globalId) {
        return jdbc.queryForObject(
                "INSERT INTO university_report (global_univ_id, status) VALUES (?, 'PUBLISHED') RETURNING id",
                Long.class, globalId);
    }
}
