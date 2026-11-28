package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.member.HideContentByWithdrawnMemberPort;
import me.yeonjae.ovlo.application.port.out.moderation.ContentReportPort;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 실 PostgreSQL + Flyway 로 신고 기록 1년 뒤 익명화와 탈퇴 시 차단 기록 삭제를 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ReportRetentionIntegrationTest {

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
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    private static final Instant CUTOFF = Instant.parse("2027-01-01T00:00:00Z");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ReportRetentionPersistenceAdapter retention;
    @Autowired private ContentReportPort contentReports;
    @Autowired private HideContentByWithdrawnMemberPort withdrawal;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long universityId;
    private Long reporter;
    private Long author;

    @BeforeEach
    void setUp() {
        // name_en 은 UNIQUE(V62)라 테스트마다 다른 이름을 쓴다.
        universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES (?) RETURNING id", Long.class,
                "Retention Test University " + SEQ.incrementAndGet());
        reporter = newMember();
        author = newMember();
    }

    private Long newMember() {
        int n = SEQ.incrementAndGet();
        Member m = Member.create("ret" + n, "이름", "Seoul", new Email("ret" + n + "@example.com"),
                new Password("hashedPassword"), new UniversityId(universityId), new Major("CS", DegreeType.BACHELOR, 1));
        return memberAdapter.save(m).getId().value();
    }

    private Long contentReport(String status, Instant createdAt, Instant resolvedAt) {
        return jdbc.queryForObject("""
                INSERT INTO content_report(target_type, target_id, target_author_id, reporter_id, reason, detail,
                                           status, created_at, resolved_at, resolved_by, resolution)
                VALUES ('POST', 77, ?, ?, 'SPAM', '광고 글입니다', ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, author, reporter, status, Timestamp.from(createdAt),
                resolvedAt == null ? null : Timestamp.from(resolvedAt),
                resolvedAt == null ? null : "admin", resolvedAt == null ? null : "HIDE_CONTENT");
    }

    private Long listing() {
        return jdbc.queryForObject("""
                INSERT INTO marketplace_listing(seller_id, title, description, source_language, category, item_condition,
                    free, bundle, partial_pickup, amount, currency, country_code, city, meeting_point)
                VALUES (?, '책상', '설명', 'ko', 'FURNITURE', 'GOOD', true, false, false, 0, 'KRW', 'KR', 'Seoul', '정문')
                RETURNING id
                """, Long.class, author);
    }

    private Long marketplaceReport(Long listingId, Long reporterId, Instant createdAt) {
        return jdbc.queryForObject("""
                INSERT INTO marketplace_report(listing_id, reporter_id, reason, created_at) VALUES (?, ?, '사기 의심', ?) RETURNING id
                """, Long.class, listingId, reporterId, Timestamp.from(createdAt));
    }

    @Test
    @DisplayName("처리 끝난 지 1년 지난 신고는 신고자·대상·사유를 지우고 유형·결과·날짜만 남긴다")
    void anonymizesResolvedContentReportsOlderThanCutoff() {
        Long old = contentReport("ACTIONED", CUTOFF.minusSeconds(86_400 * 30), CUTOFF.minusSeconds(60));
        Long recent = contentReport("DISMISSED", CUTOFF.minusSeconds(86_400 * 30), CUTOFF.plusSeconds(60));
        Long open = contentReport("OPEN", CUTOFF.minusSeconds(86_400 * 400), null);

        int count = retention.anonymizeOlderThan(CUTOFF);

        assertThat(count).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM content_report WHERE id = ?", old);
        assertThat(row.get("reporter_id")).isNull();
        assertThat(row.get("target_author_id")).isNull();
        assertThat(row.get("target_id")).isNull();
        assertThat(row.get("detail")).isNull();
        assertThat(row.get("resolved_by")).isNull();
        assertThat(row.get("anonymized_at")).isNotNull();
        assertThat(row).containsEntry("target_type", "POST").containsEntry("reason", "SPAM")
                .containsEntry("status", "ACTIONED").containsEntry("resolution", "HIDE_CONTENT");
        assertThat(jdbc.queryForObject("SELECT reporter_id FROM content_report WHERE id = ?", Long.class, recent)).isEqualTo(reporter);
        assertThat(jdbc.queryForObject("SELECT reporter_id FROM content_report WHERE id = ?", Long.class, open)).isEqualTo(reporter);
    }

    @Test
    @DisplayName("익명화된 신고는 관리자 신고 목록에 나오지 않고, 다시 돌려도 건수가 늘지 않는다")
    void anonymizedReportsDisappearFromAdminListAndRunIsIdempotent() {
        contentReport("ACTIONED", CUTOFF.minusSeconds(86_400 * 30), CUTOFF.minusSeconds(60));
        retention.anonymizeOlderThan(CUTOFF);

        assertThat(retention.anonymizeOlderThan(CUTOFF)).isZero();
        assertThat(contentReports.findByStatus(ReportStatus.ACTIONED, 100))
                .noneMatch(r -> r.targetAuthorId() == 0L || r.reporterNickname() == null);
    }

    @Test
    @DisplayName("접수한 지 1년 지난 중고거래 신고는 게시글·신고자·사유를 지운다")
    void anonymizesMarketplaceReportsOlderThanCutoff() {
        Long listingId = listing();
        Long old = marketplaceReport(listingId, reporter, CUTOFF.minusSeconds(60));
        Long other = newMember();
        Long recent = marketplaceReport(listingId, other, CUTOFF.plusSeconds(60));

        int count = retention.anonymizeOlderThan(CUTOFF);

        assertThat(count).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM marketplace_report WHERE id = ?", old);
        assertThat(row.get("listing_id")).isNull();
        assertThat(row.get("reporter_id")).isNull();
        assertThat(row.get("reason")).isNull();
        assertThat(row.get("anonymized_at")).isNotNull();
        assertThat(jdbc.queryForObject("SELECT reporter_id FROM marketplace_report WHERE id = ?", Long.class, recent)).isEqualTo(other);
    }

    @Test
    @DisplayName("탈퇴하면 그 회원이 건 차단과 그 회원을 막은 차단이 모두 지워진다")
    void withdrawalDeletesBlocksInBothDirections() {
        Long third = newMember();
        jdbc.update("INSERT INTO member_block(actor_id, target_id) VALUES (?, ?), (?, ?), (?, ?)",
                reporter, author, author, reporter, third, author);

        withdrawal.hideAllContentByMember(reporter);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_block WHERE actor_id = ? OR target_id = ?",
                Integer.class, reporter, reporter)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM member_block WHERE actor_id = ? AND target_id = ?",
                Integer.class, third, author)).isEqualTo(1);
    }
}
