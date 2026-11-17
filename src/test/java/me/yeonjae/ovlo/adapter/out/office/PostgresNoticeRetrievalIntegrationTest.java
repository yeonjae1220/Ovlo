package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.adapter.out.persistence.*;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;

@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PostgresNoticeRetrievalIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("ovlo.search.provider", () -> "postgres");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired io.micrometer.core.instrument.MeterRegistry meters;
    @Autowired OfficeNoticePersistenceAdapter notices;
    @Autowired OfficePersistenceAdapter offices;
    @Autowired MemberPersistenceAdapter members;
    @Autowired PostgresFtsNoticeSearchAdapter search;

    @Test void syntheticGoldenSetRecallAndRanks() throws Exception {
        Long university = jdbc.queryForObject("INSERT INTO global_universities (name_en) VALUES ('Synthetic Retrieval University') RETURNING id", Long.class);
        var member = Member.create("retrieval", "합성 학생", "Test", new Email("retrieval@example.test"), new Password("test-hash"),
                new UniversityId(university), new Major("CS", DegreeType.BACHELOR, 1));
        Long memberId = members.save(member).getId().value();
        Long office = offices.save(Office.create(university, "합성 국제처", NoticeRetrievalGoldenSet.NOW)).getId().value();
        Map<Long, Notice> documents = new LinkedHashMap<>();
        for (var doc : NoticeRetrievalGoldenSet.corpus().notices())
            documents.put(doc.id(), notices.save(NoticeRetrievalGoldenSet.notice(doc, office, memberId)));
        double postgresBefore = observations("POSTGRES");
        double keywordBefore = observations("KEYWORD");
        NoticeRetrievalGoldenSet.evaluate("postgres", search, office, documents);
        org.assertj.core.api.Assertions.assertThat(observations("POSTGRES") - postgresBefore).isEqualTo(70);
        org.assertj.core.api.Assertions.assertThat(observations("KEYWORD") - keywordBefore).isZero();
    }
    private double observations(String provider) {
        return meters.find("ovlo.office.search.completed").tag("provider", provider)
                .counters().stream().mapToDouble(c -> c.count()).sum();
    }
}
