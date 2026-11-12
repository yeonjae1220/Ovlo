package me.yeonjae.ovlo.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Upgrade a populated V41 database, including the intermediate V42/V43 states, without Hibernate schema creation. */
@Tag("integration")
@Testcontainers
class MarketplaceInquiryMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private Flyway migration(String target) {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target(target).load();
    }

    @Test
    void v41ToV42ToV43ToV44PreservesExistingDataAndEnforcesNewConstraints() {
        migration("41").migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        Long university = db.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Upgrade test university') RETURNING id", Long.class);
        Long member = db.queryForObject("INSERT INTO member(name, nickname, email, home_university_id) VALUES ('Student', 'upgrade-student', 'upgrade@example.test', ?) RETURNING id", Long.class, university);
        Long office = db.queryForObject("INSERT INTO office(global_univ_id, name, status) VALUES (?, 'Upgrade office', 'ACTIVE') RETURNING id", Long.class, university);
        Long root = db.queryForObject("""
                INSERT INTO office_inquiry(office_id, member_id, lang, question, outcome, answer_text, asked_at)
                VALUES (?, ?, 'ko', '기숙사 신청?', 'ANSWERED', '2월 10일까지 [1]', CURRENT_TIMESTAMP) RETURNING id
                """, Long.class, office, member);
        Long room = db.queryForObject("INSERT INTO chat_room(type, name) VALUES ('GROUP', 'Existing consultation') RETURNING id", Long.class);
        Long consultation = db.queryForObject("""
                INSERT INTO office_consultation(office_id, member_id, inquiry_id, consent_version, consent_text,
                  shared_question, shared_answer, shared_lang, shared_verified_student, status, chat_room_id, opened_at)
                VALUES (?, ?, ?, 'v1', 'Original consent', '기숙사 신청?', '2월 10일까지 [1]', 'ko', TRUE, 'OPEN', ?, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, office, member, root, room);
        var oldInquiry = db.queryForMap("SELECT question, answer_text, asked_at, outcome FROM office_inquiry WHERE id = ?", root);
        var oldConsultation = db.queryForMap("SELECT consent_version, consent_text, shared_question, shared_answer, chat_room_id FROM office_consultation WHERE id = ?", consultation);

        assertThat(migration("42").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(db.queryForObject("SELECT inactive_reason FROM global_universities WHERE id = ?", String.class, university)).isNull();
        assertThat(migration("43").migrate().migrationsExecuted).isEqualTo(1);
        Long listing = db.queryForObject("""
                INSERT INTO marketplace_listing(seller_id, title, description, source_language, category, item_condition,
                  free, bundle, partial_pickup, amount, currency, country_code, city, meeting_point)
                VALUES (?, 'Existing V43 listing', 'Keep this listing', 'en', 'HOUSEHOLD', 'GOOD',
                  FALSE, TRUE, TRUE, 1.2345, 'CLF', 'FR', 'Paris', 'Campus gate') RETURNING id
                """, Long.class, member);

        Flyway latest = migration("44");
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
        latest.validate();
        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("44");
        assertThat(db.queryForList("SELECT version FROM flyway_schema_history WHERE version IN ('42', '43', '44') AND success ORDER BY installed_rank", String.class))
                .containsExactly("42", "43", "44");
        assertThat(db.queryForMap("SELECT question, answer_text, asked_at, outcome FROM office_inquiry WHERE id = ?", root)).isEqualTo(oldInquiry);
        assertThat(db.queryForMap("SELECT consent_version, consent_text, shared_question, shared_answer, chat_room_id FROM office_consultation WHERE id = ?", consultation)).isEqualTo(oldConsultation);
        assertThat(db.queryForObject("SELECT previous_inquiry_id FROM office_inquiry WHERE id = ?", Long.class, root)).isNull();
        assertThat(db.queryForObject("SELECT shared_conversation FROM office_consultation WHERE id = ?", String.class, consultation)).isNull();
        assertThat(db.queryForObject("SELECT amount FROM marketplace_listing WHERE id = ?", BigDecimal.class, listing)).isEqualByComparingTo("1.2345");

        String insertTurn = """
                INSERT INTO office_inquiry(office_id, member_id, lang, question, outcome, asked_at, previous_inquiry_id)
                VALUES (?, ?, 'ko', '그럼 어디서?', 'NO_GROUNDS', CURRENT_TIMESTAMP, ?)
                """;
        assertThat(db.update(insertTurn, office, member, root)).isEqualTo(1);
        assertThatThrownBy(() -> db.update(insertTurn, office, member, root)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> db.update(insertTurn, office, member, Long.MAX_VALUE)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(db.update(insertTurn, office, member, null)).isEqualTo(1);
        assertThat(db.update(insertTurn, office, member, null)).isEqualTo(1);
        assertThatThrownBy(() -> db.update("UPDATE marketplace_listing SET free = TRUE WHERE id = ?", listing))
                .isInstanceOf(DataIntegrityViolationException.class);

        String transcript = "Student: 기숙사 신청?\nAI: 2월 10일까지 [1]\nStudent: 그럼 어디서?";
        db.update("UPDATE office_consultation SET shared_conversation = ? WHERE id = ?", transcript, consultation);
        assertThat(latest.migrate().migrationsExecuted).isZero();
        assertThat(db.queryForObject("SELECT shared_conversation FROM office_consultation WHERE id = ?", String.class, consultation)).isEqualTo(transcript);
        assertThat(db.queryForObject("SELECT count(*) FROM marketplace_listing WHERE id = ?", Long.class, listing)).isEqualTo(1);
    }
}
