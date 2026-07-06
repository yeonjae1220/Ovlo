package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.result.UniversityReportResult;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityReportPort;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 리포트 조회 시 대학 소재국(countryCode) 이 채워지는지 검증(디스플레이 통화 환산용).
 * 실 PostgreSQL(Testcontainers) + Flyway(V27 global_university_names 포함).
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UniversityReportPersistenceAdapterIntegrationTest {

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
    private LoadUniversityReportPort loadPort;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM university_report_translation");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("DELETE FROM global_universities");
    }

    @Test
    @DisplayName("global 대학 연결 리포트는 소재국 countryCode 가 채워진다")
    void reportCarriesCountryCode() {
        Long globalId = insertGlobal("Technische Universitaet Muenchen", "DE");
        Long reportId = insertReport(globalId);
        insertTranslation(reportId, "ko", "뮌헨공대 교환학생 가이드");

        Optional<UniversityReportResult> result = loadPort.findByIdAndLang(reportId, "ko");

        assertThat(result).isPresent();
        assertThat(result.get().countryCode()).isEqualTo("DE");
        assertThat(result.get().globalUnivId()).isEqualTo(globalId);
    }

    @Test
    @DisplayName("global 미연결 리포트는 countryCode 가 null 이다")
    void reportWithoutGlobalHasNullCountryCode() {
        Long reportId = insertReportWithoutGlobal();
        insertTranslation(reportId, "ko", "제목");

        Optional<UniversityReportResult> result = loadPort.findByIdAndLang(reportId, "ko");

        assertThat(result).isPresent();
        assertThat(result.get().countryCode()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long insertGlobal(String nameEn, String countryCode) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, country_code) VALUES (?,?) RETURNING id",
                Long.class, nameEn, countryCode);
    }

    private Long insertReport(Long globalId) {
        return jdbc.queryForObject(
                "INSERT INTO university_report (global_univ_id, status, supported_langs) "
                        + "VALUES (?, 'PUBLISHED', '{ko}') RETURNING id",
                Long.class, globalId);
    }

    private Long insertReportWithoutGlobal() {
        return jdbc.queryForObject(
                "INSERT INTO university_report (status, supported_langs) "
                        + "VALUES ('PUBLISHED', '{ko}') RETURNING id",
                Long.class);
    }

    private void insertTranslation(Long reportId, String lang, String title) {
        jdbc.update(
                "INSERT INTO university_report_translation (report_id, lang, title, body, content) "
                        + "VALUES (?,?,?,?, '{}'::jsonb)",
                reportId, lang, title, "본문");
    }
}
