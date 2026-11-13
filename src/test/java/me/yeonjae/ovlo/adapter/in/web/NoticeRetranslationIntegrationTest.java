package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.adapter.out.persistence.*;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Exercises the real transaction proxies and PostgreSQL persistence, with deterministic provider output. */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NoticeRetranslationIntegrationTest {
    @Container static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.open-in-view", () -> "false");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberPersistenceAdapter members;
    @Autowired OfficePersistenceAdapter offices;
    @Autowired OfficeNoticePersistenceAdapter notices;
    @Autowired JwtTokenProvider jwt;
    @Autowired WebApplicationContext context;
    @MockitoBean TranslationPort translator;
    MockMvc mvc;
    Notice notice;
    String path, token;
    final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Long university = jdbc.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Retranslation tests') RETURNING id", Long.class);
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Long actor = members.save(Member.create(unique, "담당자", "Seoul", new Email(unique + "@example.com"), new Password("hashed"),
                new UniversityId(university), new Major("IA", DegreeType.BACHELOR, 4))).getId().value();
        Office office = Office.create(university, "국제처", now);
        office.invite(actor, OfficeRole.OFFICE_ADMIN, "test", now); office.accept(actor, now);
        Long officeId = offices.save(office).getId().value();
        notice = notices.save(Notice.register(officeId, NoticeCategory.OTHER, "교육 안내", "교육은 1.5시간입니다.", null, List.of(),
                "2026-2", today.minusDays(1), today.plusDays(30), actor, now));
        path = "/api/v1/office-admin/" + officeId + "/notices/" + notice.getId();
        token = "Bearer " + jwt.generateAccessToken(new MemberId(actor), MemberRole.MEMBER, new OfficeMembership(officeId, OfficeRole.OFFICE_ADMIN));
        when(translator.translate(any())).thenAnswer(inv -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).as("No DB transaction during paid provider call").isFalse();
            TranslationPort.Request request = inv.getArgument(0);
            String value = switch (request.to()) { case DE, FR, VI -> "1,5"; default -> "1.5"; };
            return new TranslationPort.Result("[" + request.to() + "] Training", "Duration: " + value, "test");
        });
    }

    JsonNode retranslate(long revision) throws Exception {
        var response = mvc.perform(post(path + "/retranslate").header("Authorization", token).contentType("application/json")
                        .content("{\"revision\":" + revision + "}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString());
    }

    JsonNode freshGet() throws Exception {
        return json.readTree(mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test void immediateResponseMatchesCommittedTranslationsAndRevision() throws Exception {
        JsonNode immediate = retranslate(notice.getRowVersion());
        JsonNode fresh = freshGet();
        assertThat(fresh.path("translations").size()).isEqualTo(6); // Writes actually committed, independent of response assertions.
        assertThat(immediate).isEqualTo(fresh);
        assertThat(immediate.path("missingLangs").size()).isZero();
        assertThat(immediate.path("revision").asLong()).isGreaterThan(notice.getRowVersion());
        for (JsonNode translation : immediate.path("translations")) {
            assertThat(translation.path("status").asText()).isEqualTo("MACHINE");
            assertThat(translation.path("stale").asBoolean()).isFalse();
        }
        verify(translator, times(6)).translate(any());
        clearInvocations(translator);
        assertThat(retranslate(immediate.path("revision").asLong())).isEqualTo(fresh);
        verifyNoInteractions(translator); // The revision in the response is usable; no redundant paid calls.
    }

    @Test void partialFailureReportsOnlyTheMissingLanguageImmediately() throws Exception {
        doAnswer(inv -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            TranslationPort.Request request = inv.getArgument(0);
            String value = switch (request.to()) { case DE -> "15"; case FR, VI -> "1,5"; default -> "1.5"; };
            return new TranslationPort.Result("[" + request.to() + "] Training", "Duration: " + value, "test");
        }).when(translator).translate(any());
        JsonNode immediate = retranslate(notice.getRowVersion());
        JsonNode fresh = freshGet();
        assertThat(fresh.path("translations").size()).isEqualTo(5);
        assertThat(immediate).isEqualTo(fresh);
        assertThat(immediate.path("missingLangs").toString()).isEqualTo("[\"DE\"]");
    }

    @Test void staleRevisionAndWrongOfficeAreRejectedBeforeProviderCalls() throws Exception {
        mvc.perform(post(path + "/retranslate").header("Authorization", token).contentType("application/json")
                .content("{\"revision\":99999}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/office-admin/" + (notice.getOfficeId() + 1) + "/notices/" + notice.getId() + "/retranslate")
                .header("Authorization", token).contentType("application/json").content("{\"revision\":0}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(translator);
    }
}
