package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 본교 선택 검색(회원가입·온보딩, /api/v1/global-universities)이 영문명 외의 이름으로도
 * 찾아지는지 실 PostgreSQL(Testcontainers) + Flyway 로 검증.
 *
 * <p>검증 대상:
 * <ul>
 *   <li>현지어명(local_name)·다국어 이름(global_university_names)·연결 교환대학 한글명(name_ko)</li>
 *   <li>숨긴 중복 행의 현지어명·다국어 이름으로 찾아도 대표 행이 나온다</li>
 *   <li>건수(count)가 목록과 같은 조건을 쓴다</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UniversitySearchMultilingualIntegrationTest {

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
    private JdbcTemplate jdbc;

    private Long snuId;        // local_name 만 보유
    private Long pusanId;      // exchange.name_ko 로만 한글명 보유
    private Long tumId;        // 다국어 이름(ja) 보유
    private Long canonicalId;  // 대표 행
    private Long hiddenId;     // 숨긴 중복 행 — 현지어명·다국어 이름 보유

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM exchange_video_reviews");
        jdbc.update("DELETE FROM university_report");
        jdbc.update("DELETE FROM exchange_universities");
        jdbc.update("DELETE FROM global_university_names");
        jdbc.update("UPDATE global_universities SET canonical_id = NULL");
        jdbc.update("DELETE FROM global_universities");

        snuId = insertGlobal("Seoul National University", "서울대학교", "KR");
        pusanId = insertGlobal("Pusan National University", null, "KR");
        insertExchange(pusanId, "부산대학교", "Pusan National University");
        tumId = insertGlobal("Technical University of Munich", null, "DE");
        insertGlobalName(tumId, "ja", "ミュンヘン工科大学");

        canonicalId = insertGlobal("KU Leuven", null, "BE");
        hiddenId = insertGlobal("Katholieke Universiteit Leuven", "루벤가톨릭대학교", "BE");
        jdbc.update("UPDATE global_universities SET canonical_id = ? WHERE id = ?", canonicalId, hiddenId);
        insertGlobalName(hiddenId, "nl", "Katholieke Hogeschool Leuven-oud");
    }

    @Test
    @DisplayName("현지어명(local_name)으로 찾는다")
    void matchesLocalName() {
        assertThat(searchPort.search("서울대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(snuId);
        assertThat(searchPort.count("서울대", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("연결된 교환대학의 한글명(name_ko)으로 찾는다")
    void matchesExchangeKoreanName() {
        assertThat(searchPort.search("부산대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(pusanId);
        assertThat(searchPort.count("부산대", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("다국어 이름(global_university_names)으로 찾는다")
    void matchesMultilingualName() {
        assertThat(searchPort.search("ミュンヘン", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(tumId);
        assertThat(searchPort.count("ミュンヘン", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("영문명 검색은 그대로 동작하고 다른 언어 이름과 섞이지 않는다")
    void englishStillWorks() {
        assertThat(searchPort.search("Munich", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(tumId);
        assertThat(searchPort.search("없는대학", null, 0, 20)).isEmpty();
        assertThat(searchPort.count("없는대학", null)).isZero();
    }

    @Test
    @DisplayName("숨긴 중복 행의 현지어명·다국어 이름으로 찾아도 대표 행이 나온다")
    void hiddenRowNamesResolveToCanonical() {
        for (String keyword : new String[] {"루벤가톨릭", "Hogeschool Leuven-oud", "Katholieke"}) {
            assertThat(searchPort.search(keyword, null, 0, 20)).as(keyword)
                    .extracting(u -> u.getId().value()).containsExactly(canonicalId);
            assertThat(searchPort.count(keyword, null)).as(keyword).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("countryCode 필터는 다국어 매칭과 함께 적용된다")
    void countryFilterCombinesWithMultilingualMatch() {
        assertThat(searchPort.search("서울대", "DE", 0, 20)).isEmpty();
        assertThat(searchPort.search("서울대", "KR", 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(snuId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Long insertGlobal(String nameEn, String localName, String code) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, local_name, country_code) VALUES (?,?,?) RETURNING id",
                Long.class, nameEn, localName, code);
    }

    private void insertExchange(Long globalId, String nameKo, String nameEn) {
        jdbc.update("INSERT INTO exchange_universities (global_univ_id, name_ko, name_en, country, country_code, city) "
                        + "VALUES (?,?,?,?,?,?)",
                globalId, nameKo, nameEn, "대한민국", "KR", "Busan");
    }

    private void insertGlobalName(Long globalId, String lang, String name) {
        jdbc.update("INSERT INTO global_university_names (global_univ_id, lang, name) VALUES (?,?,?)",
                globalId, lang, name);
    }
}
