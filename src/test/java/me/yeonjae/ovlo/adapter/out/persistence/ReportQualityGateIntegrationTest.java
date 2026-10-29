package me.yeonjae.ovlo.adapter.out.persistence;

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
 * 리포트 번역 품질 게이트 트리거(V28–V30, V33)를 실 PostgreSQL + Flyway 로 검증.
 *
 * <p>트리거는 주입을 막지 않고 결함만 report_quality_issue 에 기록한다. 재생성 리포트는
 * content.sources 에 출처 제목을 원문 그대로(예: 중국어 영상 제목) 두므로, 출처 목록은
 * 언어 섞임 검사에서 제외해야 한다. 본문의 섞임은 계속 잡혀야 한다.</p>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReportQualityGateIntegrationTest {

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

    private static final String SOURCES_WITH_CHINESE_TITLE = """
            {"visa": {"type": "Residence permit"},
             "sources": [{"n": 1, "title": "【在校生经验分享】慕尼黑工业大学TUM机器人专业", "url": "https://www.youtube.com/watch?v=x", "type": "video"}]}
            """;

    @Autowired
    private JdbcTemplate jdbc;

    private Long reportId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM report_quality_issue");
        jdbc.update("DELETE FROM university_report_translation");
        jdbc.update("DELETE FROM university_report");
        reportId = jdbc.queryForObject(
                "INSERT INTO university_report (status) VALUES ('PUBLISHED') RETURNING id", Long.class);
    }

    @Test
    @DisplayName("출처 목록의 원문 제목은 언어 섞임 결함으로 기록되지 않는다")
    void sourceTitlesAreNotDefects() {
        insertTranslation("en", "Exchange students apply by 10 May.", SOURCES_WITH_CHINESE_TITLE);

        assertThat(defects("en")).isEmpty();
    }

    @Test
    @DisplayName("본문에 섞인 다른 문자는 계속 결함으로 기록된다")
    void bodyScriptMixIsStillDetected() {
        insertTranslation("en", "Students eat at the Mensa食堂 daily.", SOURCES_WITH_CHINESE_TITLE);

        assertThat(defects("en")).containsExactly("script_mix_in_latin");
    }

    @Test
    @DisplayName("출처 밖 content 필드의 섞임도 계속 결함으로 기록된다")
    void contentScriptMixOutsideSourcesIsStillDetected() {
        insertTranslation("en", "Clean body.", """
                {"dining": {"must_try_foods": ["Bratwurst떡볶이"]}, "sources": []}
                """);

        assertThat(defects("en")).containsExactly("script_mix_in_latin");
    }

    private void insertTranslation(String lang, String body, String content) {
        jdbc.update("INSERT INTO university_report_translation (report_id, lang, title, body, content) "
                + "VALUES (?, ?, 'Guide', ?, ?::jsonb)", reportId, lang, body, content);
    }

    private List<String> defects(String lang) {
        return jdbc.query("SELECT unnest(defects) FROM report_quality_issue WHERE report_id = ? AND lang = ?",
                (rs, i) -> rs.getString(1), reportId, lang);
    }
}
