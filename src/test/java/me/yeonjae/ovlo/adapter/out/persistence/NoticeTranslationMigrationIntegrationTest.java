package me.yeonjae.ovlo.adapter.out.persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;

@Tag("integration") @Testcontainers
class NoticeTranslationMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> PG=new PostgreSQLContainer<>("postgres:16");
    Flyway migration(String version) { return Flyway.configure().dataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword()).locations("classpath:db/migration").target(version).load(); }
    @Test void upgradeQuarantinesMachineTranslationsAndQueuesRecoveryWithoutChangingReviewedText() {
        migration("47").migrate();
        var db=new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(),PG.getUsername(),PG.getPassword()));
        Long university=db.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Translation upgrade') RETURNING id",Long.class);
        Long member=db.queryForObject("INSERT INTO member(name,nickname,email,home_university_id) VALUES ('Staff','translation-upgrade','translation-upgrade@example.test',?) RETURNING id",Long.class,university);
        Long office=db.queryForObject("INSERT INTO office(global_univ_id,name,status) VALUES (?,'Office','PILOT') RETURNING id",Long.class,university);
        Long notice=db.queryForObject("""
                INSERT INTO office_notice(office_id,category,title,body,term,effective_from,expires_at,status,approved_by,approved_at,created_by)
                VALUES (?,'OTHER','Original title','Original body','2026-2',CURRENT_DATE,CURRENT_DATE+30,'APPROVED','staff',CURRENT_TIMESTAMP,?) RETURNING id
                """,Long.class,office,member);
        for(String lang:java.util.List.of("en","de")) db.update("""
                INSERT INTO office_notice_translation(notice_id,lang,title,body,status,source_version,generated_at)
                VALUES (?,?,'Original translated title','Original translated body',?,1,CURRENT_TIMESTAMP)
                """,notice,lang,lang.equals("en")?"MACHINE":"REVIEWED");
        var before=db.queryForList("SELECT lang,title,body,status,source_version,generated_at FROM office_notice_translation ORDER BY lang");
        assertThat(migration("48").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(db.queryForList("SELECT lang,title,body,status,source_version,generated_at FROM office_notice_translation ORDER BY lang")).isEqualTo(before);
        assertThat(db.queryForList("SELECT integrity_verified FROM office_notice_translation",Boolean.class)).containsOnly(false);
        assertThat(db.queryForList("SELECT lang FROM notice_translation_job WHERE notice_id=?",String.class,notice)).containsExactlyInAnyOrder("en","fr","ja","zh","vi");
        assertThat(new NoticeTranslationJobAdapter(db).dueNotices()).containsExactly(notice);
        assertThat(migration("48").migrate().migrationsExecuted).isZero(); migration("48").validate();
        db.update("UPDATE notice_translation_job SET state='CANCELLED',attempts=1,available_at=CURRENT_TIMESTAMP+INTERVAL '1 hour' WHERE notice_id=?",notice);
        db.update("UPDATE notice_translation_job SET attempts=3 WHERE lang='ja'");
        db.update("UPDATE notice_translation_job SET created_at=CURRENT_TIMESTAMP-INTERVAL '49 hours' WHERE lang='fr'");
        db.update("UPDATE notice_translation_job SET source_version=0 WHERE lang='zh'");
        var budgetBefore=db.queryForList("SELECT lang,attempts,available_at,created_at FROM notice_translation_job ORDER BY lang");
        assertThat(migration("49").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(db.queryForList("SELECT lang FROM notice_translation_job WHERE state='PAUSED'",String.class)).containsExactlyInAnyOrder("en","vi");
        assertThat(db.queryForList("SELECT lang,attempts,available_at,created_at FROM notice_translation_job ORDER BY lang")).isEqualTo(budgetBefore);
        assertThat(new NoticeTranslationJobAdapter(db).dueNotices()).isEmpty(); // original cooldown remains
        assertThat(db.queryForObject("SELECT state FROM notice_translation_job WHERE lang='en'",String.class)).isEqualTo("PENDING");
        assertThat(migration("49").migrate().migrationsExecuted).isZero(); migration("49").validate();
    }
}
