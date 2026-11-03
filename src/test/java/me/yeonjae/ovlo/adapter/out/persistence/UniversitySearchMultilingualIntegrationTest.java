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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 본교 선택 검색(회원가입·온보딩, /api/v1/global-universities)이 영문명 외의 이름으로도
 * 찾아지는지 실 PostgreSQL(Testcontainers) + Flyway 로 검증.
 *
 * <p>검증 대상:
 * <ul>
 *   <li>현지어명(local_name)·다국어 이름(global_university_names)·연결 교환대학 한글명(name_ko)</li>
 *   <li>숨긴 중복 행의 현지어명·다국어 이름으로 찾아도 대표 행이 나온다</li>
 *   <li>별칭(global_university_aliases: 약어·옛 이름)으로 찾고, 숨긴 행의 별칭은 대표 행으로</li>
 *   <li>표시 언어 이름 조회(findLocalizedNames) — 다국어 라벨, ko 는 교환대학 한글명 폴백</li>
 *   <li>V40 데이터 마이그레이션이 도메인으로 한글명·중복·별칭을 채운다</li>
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
        jdbc.update("DELETE FROM global_university_aliases");
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
    @DisplayName("별칭(약어·옛 이름)으로 찾고, 숨긴 중복 행의 별칭은 대표 행을 돌려준다")
    void matchesAliases() {
        insertAlias(pusanId, "부산대");          // 이미 name_ko 로도 잡히는 약어
        insertAlias(tumId, "뮌헨공대");
        insertAlias(tumId, "TUM");
        insertAlias(hiddenId, "루벤가톨릭대");    // 숨긴 행의 별칭 → 대표 행

        assertThat(searchPort.search("뮌헨공대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(tumId);
        assertThat(searchPort.search("tum", null, 0, 20))        // ILIKE: 대소문자 무시
                .extracting(u -> u.getId().value()).containsExactly(tumId);
        assertThat(searchPort.count("tum", null)).isEqualTo(1L);
        assertThat(searchPort.search("루벤가톨릭대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(canonicalId);
        assertThat(searchPort.count("루벤가톨릭대", null)).isEqualTo(1L);
    }

    @Test
    @DisplayName("표시 언어 이름: 다국어 라벨을 돌려주고, ko 는 교환대학 한글명으로 폴백하며, 없는 대학은 맵에 없다")
    void localizedNames() {
        insertGlobalName(tumId, "ko", "뮌헨공과대학교");
        var ids = java.util.List.of(snuId, pusanId, tumId, canonicalId);

        Map<Long, String> ja = searchPort.findLocalizedNames(ids, "ja");
        assertThat(ja).containsExactly(Map.entry(tumId, "ミュンヘン工科大学"));

        Map<Long, String> ko = searchPort.findLocalizedNames(ids, "ko");
        assertThat(ko).containsEntry(tumId, "뮌헨공과대학교")
                .containsEntry(pusanId, "부산대학교")      // 다국어 테이블에 없어 exchange.name_ko 폴백
                .doesNotContainKeys(snuId, canonicalId);    // local_name 만 있는 행은 폴백하지 않는다

        assertThat(searchPort.findLocalizedNames(java.util.List.of(), "ja")).isEmpty();
        assertThat(searchPort.findLocalizedNames(ids, " ")).isEmpty();
    }

    @Test
    @DisplayName("V40 데이터 마이그레이션은 도메인으로 한글명·중복·별칭을 채우고 재실행해도 안전하다")
    void v40SeedByDomain() {
        // 운영 마스터와 같은 도메인으로 행을 만들고 V40 본문을 다시 실행한다(마이그레이션은 빈 DB 에서 no-op 였다).
        Long seoultech = insertGlobal("Seoul National University of Science and Technology", null, "KR", "seoultech.ac.kr");
        Long snut = insertGlobal("Seoul National University of Technology", null, "KR", "snut.ac.kr");
        Long scu = insertGlobal("Seoul Christian University", null, "KR", "scu.ac.kr");
        Long koreaU = insertGlobal("Korea University", "고려대학교", "KR", "korea-u.ac.jp");
        insertGlobalName(scu, "ko", "Seoul Christian University");   // 라틴 문자뿐인 ko 라벨 → 교체 대상

        String v40 = readMigration("V40__kr_university_names_duplicates_aliases.sql");
        jdbc.execute(v40);
        jdbc.execute(v40);   // 재실행

        assertThat(jdbc.queryForObject("SELECT name FROM global_university_names WHERE global_univ_id = ? AND lang = 'ko'",
                String.class, snut)).isEqualTo("서울산업대학교");
        assertThat(jdbc.queryForObject("SELECT name||'/'||source FROM global_university_names WHERE global_univ_id = ? AND lang = 'ko'",
                String.class, scu)).isEqualTo("서울기독대학교/manual");
        assertThat(jdbc.queryForObject("SELECT canonical_id FROM global_universities WHERE id = ?", Long.class, snut))
                .isEqualTo(seoultech);
        assertThat(jdbc.queryForObject("SELECT canonical_id FROM global_universities WHERE id = ?", Long.class, seoultech))
                .isNull();
        assertThat(jdbc.queryForList("SELECT alias FROM global_university_aliases WHERE global_univ_id = ? ORDER BY alias",
                String.class, seoultech)).containsExactly("과기대", "서울과기대", "서울산업대");
        assertThat(jdbc.queryForObject("SELECT domain||'|'||website FROM global_universities WHERE id = ?", String.class, koreaU))
                .isEqualTo("korea.ac.kr|https://www.korea.ac.kr");

        // 숨긴 옛 이름 행의 한글명·별칭으로 찾으면 대표 행 하나만 나온다
        assertThat(searchPort.search("서울산업대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(seoultech);
        assertThat(searchPort.search("서울과기대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(seoultech);
        assertThat(searchPort.search("서울기독대", null, 0, 20))
                .extracting(u -> u.getId().value()).containsExactly(scu);
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
        return insertGlobal(nameEn, localName, code, null);
    }

    private Long insertGlobal(String nameEn, String localName, String code, String domain) {
        return jdbc.queryForObject(
                "INSERT INTO global_universities (name_en, local_name, country_code, domain) VALUES (?,?,?,?) RETURNING id",
                Long.class, nameEn, localName, code, domain);
    }

    private void insertAlias(Long globalId, String alias) {
        jdbc.update("INSERT INTO global_university_aliases (global_univ_id, alias) VALUES (?,?)", globalId, alias);
    }

    private static String readMigration(String file) {
        try (var in = UniversitySearchMultilingualIntegrationTest.class.getResourceAsStream("/db/migration/" + file)) {
            return new String(java.util.Objects.requireNonNull(in, file).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
