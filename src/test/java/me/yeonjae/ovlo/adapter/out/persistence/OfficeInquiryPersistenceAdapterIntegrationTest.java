package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.office.PostgresFtsNoticeSearchAdapter;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 실 PostgreSQL + Flyway(V36) 로 문의 영속과 전문 검색 어댑터(ADR-0006)를 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficeInquiryPersistenceAdapterIntegrationTest {

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
        registry.add("ovlo.search.provider", () -> "postgres");
    }

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 2);
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private OfficeInquiryPersistenceAdapter adapter;
    @Autowired private OfficeNoticePersistenceAdapter noticeAdapter;
    @Autowired private OfficePersistenceAdapter officeAdapter;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private PostgresFtsNoticeSearchAdapter searchAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long officeId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        Long universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Inquiry Test University') RETURNING id", Long.class);
        int n = SEQ.incrementAndGet();
        Member m = Member.create("asker" + n, "학생", "Hanoi", new Email("asker" + n + "@example.com"),
                new Password("hashedPassword"), new UniversityId(universityId), new Major("CS", DegreeType.BACHELOR, 1));
        memberId = memberAdapter.save(m).getId().value();
        officeId = officeAdapter.save(Office.create(universityId, "국제처", NOW)).getId().value();
    }

    private Notice approvedNotice(String title, String body) {
        Notice n = Notice.register(officeId, NoticeCategory.HOUSING, title, body, null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), memberId, NOW);
        n.approve(String.valueOf(memberId), NOW, LocalDate.of(2027, 2, 1));
        return noticeAdapter.save(n);
    }

    @Test
    @DisplayName("문의·피드백을 저장하고 Office 로그·일일 집계로 읽는다 (피드백은 덮어쓴다)")
    void saveAndQuery() {
        Inquiry answered = adapter.save(Inquiry.answered(officeId, memberId, NoticeLang.VI, "Khi nào?", "10/2 [1]",
                List.of(1L), List.of(1L, 2L), NOW, 800, "claude-test"));
        adapter.save(Inquiry.noGrounds(officeId, memberId, NoticeLang.EN, "Can I work?", List.of(), NOW.plusSeconds(60), 200, null));

        adapter.saveFeedback(new InquiryFeedback(answered.getId(), false, "틀림", NOW));
        adapter.saveFeedback(new InquiryFeedback(answered.getId(), true, null, NOW.plusSeconds(5)));

        assertThat(adapter.countByOfficeSince(officeId, NOW)).isEqualTo(2);
        assertThat(adapter.countByOfficeSince(officeId, NOW.plusSeconds(30))).isEqualTo(1);
        assertThat(adapter.findByOffice(officeId, InquiryOutcome.ANSWERED, null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(adapter.findByOffice(officeId, null, NOW.plusSeconds(30), null, PageRequest.of(0, 10)).getContent())
                .extracting(Inquiry::getOutcome).containsExactly(InquiryOutcome.NO_GROUNDS);
        Inquiry reloaded = adapter.findById(answered.getId()).orElseThrow();
        assertThat(reloaded.getCitedNoticeIds()).containsExactly(1L);
        assertThat(reloaded.getCandidateNoticeIds()).containsExactly(1L, 2L);
        assertThat(adapter.findFeedback(answered.getId())).hasValueSatisfying(f -> {
            assertThat(f.helpful()).isTrue();
            assertThat(f.reason()).isNull();
        });
    }

    @Test
    @DisplayName("PostgreSQL 전문 검색: 원문 또는 학생 언어 번역에 걸리는 승인 공지만, 겹침이 큰 순으로")
    void fullTextSearch() {
        Notice dorm = approvedNotice("Dormitory application", "Dormitory applications close on February 10.");
        Notice visa = approvedNotice("Visa extension", "Visit the immigration office to extend your stay.");
        Notice koOnly = approvedNotice("식당 운영", "학생 식당 운영 시간 안내");
        Notice reloaded = noticeAdapter.findById(koOnly.getId()).orElseThrow();
        reloaded.putTranslation(NoticeLang.EN, "Cafeteria hours", "Student cafeteria opening hours", NOW);
        noticeAdapter.save(reloaded);

        assertThat(searchAdapter.findCandidates(officeId, NoticeLang.EN, "when does the dormitory application close", TODAY, 8))
                .extracting(Notice::getId).containsExactly(dorm.getId());
        assertThat(searchAdapter.findCandidates(officeId, NoticeLang.EN, "cafeteria opening hours", TODAY, 8))
                .extracting(Notice::getId).containsExactly(koOnly.getId());
        assertThat(searchAdapter.findCandidates(officeId, NoticeLang.EN, "scholarship", TODAY, 8)).isEmpty();
        assertThat(searchAdapter.findCandidates(officeId, NoticeLang.EN, "extend stay immigration", TODAY, 8))
                .extracting(Notice::getId).containsExactly(visa.getId());
    }
}
