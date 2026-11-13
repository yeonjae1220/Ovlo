package me.yeonjae.ovlo.adapter.out.persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
@Tag("integration") @Testcontainers
class NoticeSourceMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> PG=new PostgreSQLContainer<>("postgres:16");
    Flyway migration(String version) { return Flyway.configure().dataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword()).locations("classpath:db/migration").target(version).load(); }
    @Test void upgradePreservesLegacyApprovalWithoutInventingReviewOrCitations() {
        migration("46").migrate();
        var db=new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword()));
        Long university=db.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Legacy source') RETURNING id",Long.class);
        Long member=db.queryForObject("INSERT INTO member(name,nickname,email,home_university_id) VALUES ('Student','source-upgrade','source-upgrade@example.test',?) RETURNING id",Long.class,university);
        Long office=db.queryForObject("INSERT INTO office(global_univ_id,name,status) VALUES (?,'Office','ACTIVE') RETURNING id",Long.class,university);
        Long notice=db.queryForObject("""
                INSERT INTO office_notice(office_id,category,title,body,term,effective_from,expires_at,status,approved_by,approved_at,created_by)
                VALUES (?,'OTHER','Original title','Original body','2027-1','2027-01-01','2027-06-30','APPROVED','legacy-approver',CURRENT_TIMESTAMP,?) RETURNING id
                """,Long.class,office,member);
        Long inquiry=db.queryForObject("INSERT INTO office_inquiry(office_id,member_id,lang,question,outcome,answer_text,asked_at) VALUES (?,?,'ko','질문','ANSWERED','과거 답변',CURRENT_TIMESTAMP) RETURNING id",Long.class,office,member);
        var before=db.queryForMap("SELECT title,body,status,approved_by,approved_at FROM office_notice WHERE id=?",notice);
        assertThat(migration("47").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(db.queryForMap("SELECT title,body,status,approved_by,approved_at FROM office_notice WHERE id=?",notice)).isEqualTo(before);
        assertThat(db.queryForObject("SELECT source_metadata FROM office_notice WHERE id=?",String.class,notice)).isNull();
        assertThat(db.queryForObject("SELECT next_review_on FROM office_notice WHERE id=?",java.sql.Date.class,notice)).isNull();
        assertThat(db.queryForObject("SELECT count(*) FROM office_inquiry_evidence WHERE id=?",Integer.class,inquiry)).isZero();
        assertThat(db.queryForObject("SELECT count(*) FROM office_notice_audit",Integer.class)).isZero();
        assertThat(migration("47").migrate().migrationsExecuted).isZero();migration("47").validate();
    }
}
