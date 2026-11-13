package me.yeonjae.ovlo.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;

@Tag("integration") @Testcontainers
class LegacyDevelopmentAccountMigrationIntegrationTest {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @Test void upgradeDisablesLegacyIdentityAndPreservesOtherAccountsAndHistory() {
        var ds = new DriverManagerDataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        var db = new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("45").load().migrate();
        assertThat(db.queryForObject("SELECT count(*) FROM member WHERE email='dev@dev.com'",Long.class)).isZero();
        long school=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Test','KR') RETURNING id",Long.class);
        db.update("""
            INSERT INTO member(nickname,name,hometown,email,password,home_university_id,major_name,degree_type,grade_level,status,provider)
            VALUES('legacy','Legacy','Test','dev@dev.com','old-hash',?,'CS','BACHELOR',1,'ACTIVE','LOCAL'),
                  ('normal','Normal','Test','normal@example.invalid','unchanged-hash',?,'CS','BACHELOR',1,'ACTIVE','LOCAL')
            """,school,school);
        long legacy=db.queryForObject("SELECT id FROM member WHERE email='dev@dev.com'",Long.class);
        long normal=db.queryForObject("SELECT id FROM member WHERE email='normal@example.invalid'",Long.class);
        long room=db.queryForObject("INSERT INTO chat_room(type,name,created_at) VALUES('GROUP','History',now()) RETURNING id",Long.class);
        db.update("INSERT INTO chat_room_participant(chat_room_id,member_id) VALUES(?,?),(?,?)",room,legacy,room,normal);
        db.update("INSERT INTO message(chat_room_id,sender_id,content,sent_at) VALUES(?,?,'preserved',now())",room,legacy);
        var current=Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();
        current.migrate(); current.validate(); current.migrate();
        assertThat(db.queryForObject("SELECT status FROM member WHERE id=?",String.class,legacy)).isEqualTo("SUSPENDED");
        assertThat(db.queryForObject("SELECT password FROM member WHERE id=?",String.class,legacy)).startsWith("!disabled-");
        assertThat(db.queryForObject("SELECT password FROM member WHERE id=?",String.class,normal)).isEqualTo("unchanged-hash");
        assertThat(db.queryForObject("SELECT status FROM member WHERE id=?",String.class,normal)).isEqualTo("ACTIVE");
        assertThat(db.queryForObject("SELECT count(*) FROM message WHERE sender_id=?",Long.class,legacy)).isEqualTo(1);
    }
}
