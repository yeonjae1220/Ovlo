package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.Partner;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** 실 PostgreSQL + Flyway(V38) 로 제휴 영속·클릭 집계·대시보드 집계 쿼리를 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficeStatsPersistenceAdapterIntegrationTest {

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

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final Instant FROM = NOW.minusSeconds(86_400);
    private static final Instant TO = NOW.plusSeconds(86_400);
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private OfficeStatsPersistenceAdapter stats;
    @Autowired private PartnerPersistenceAdapter partnerAdapter;
    @Autowired private OfficePersistenceAdapter officeAdapter;
    @Autowired private OfficeInquiryPersistenceAdapter inquiryAdapter;
    @Autowired private OfficeConsultationPersistenceAdapter consultationAdapter;
    @Autowired private OfficeNoticePersistenceAdapter noticeAdapter;
    @Autowired private ChatPersistenceAdapter chatAdapter;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long officeId;
    private Long studentId;
    private Long staffId;

    @BeforeEach
    void setUp() {
        Long universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Stats Test University') RETURNING id", Long.class);
        studentId = newMember(universityId);
        staffId = newMember(universityId);
        officeId = officeAdapter.save(Office.create(universityId, "국제처", NOW)).getId().value();
    }

    private Long newMember(Long universityId) {
        int n = SEQ.incrementAndGet();
        Member m = Member.create("stat" + n, "이름", "Seoul", new Email("stat" + n + "@example.com"),
                new Password("hashedPassword"), new UniversityId(universityId), new Major("CS", DegreeType.BACHELOR, 1));
        return memberAdapter.save(m).getId().value();
    }

    @Test
    @DisplayName("Office 제휴 설정은 쉼표 문자열로 저장되고 그대로 복원된다; 제휴와 클릭은 분류별로 집계된다")
    void partnerSettingsAndClicks() {
        Office office = officeAdapter.findById(officeId).orElseThrow();
        office.setPartnerEnabled(PartnerCategory.SIM, true);
        office.setPartnerEnabled(PartnerCategory.PICKUP, true);
        officeAdapter.save(office);
        assertThat(officeAdapter.findById(officeId).orElseThrow().getEnabledPartnerCategories())
                .isEqualTo(Set.of(PartnerCategory.SIM, PartnerCategory.PICKUP));

        Partner sim = partnerAdapter.save(Partner.create(PartnerCategory.SIM, "KT", "설명", "https://kt.example", 0, NOW));
        Partner pickup = partnerAdapter.save(Partner.create(PartnerCategory.PICKUP, "픽업", null, "https://pick.example", 1, NOW));
        assertThat(partnerAdapter.findEnabled()).isEmpty();
        Partner reloaded = partnerAdapter.findById(sim.getId()).orElseThrow();
        reloaded.enable();
        partnerAdapter.save(reloaded);
        assertThat(partnerAdapter.findEnabled()).extracting(Partner::getId).containsExactly(sim.getId());
        assertThat(partnerAdapter.findAll()).extracting(Partner::getId).containsExactly(sim.getId(), pickup.getId());

        partnerAdapter.recordClick(sim.getId(), officeId, studentId, NOW);
        partnerAdapter.recordClick(sim.getId(), officeId, null, NOW.plusSeconds(1));
        partnerAdapter.recordClick(pickup.getId(), officeId, studentId, NOW.plusSeconds(2));
        partnerAdapter.recordClick(pickup.getId(), officeId, studentId, TO.plusSeconds(10)); // 기간 밖

        Map<PartnerCategory, Long> clicks = stats.countPartnerClicks(officeId, FROM, TO);
        assertThat(clicks).containsEntry(PartnerCategory.SIM, 2L).containsEntry(PartnerCategory.PICKUP, 1L);
    }

    @Test
    @DisplayName("문의·피드백·상담·공지 집계 쿼리가 기간과 Office 로 잘린다")
    void dashboardAggregates() {
        Inquiry a = inquiryAdapter.save(Inquiry.answered(officeId, studentId, NoticeLang.VI, "Khi nào?", "10/2 [1]",
                List.of(1L), List.of(1L), NOW, 800, "claude-test"));
        inquiryAdapter.save(Inquiry.noGrounds(officeId, studentId, NoticeLang.EN, "Can I work?", List.of(), NOW, 200, null));
        inquiryAdapter.save(Inquiry.noGrounds(officeId, studentId, NoticeLang.EN, "can i work", List.of(), NOW.plusSeconds(5), 200, null));
        inquiryAdapter.save(Inquiry.noGrounds(officeId, studentId, NoticeLang.EN, "old", List.of(), FROM.minusSeconds(10), 200, null));
        inquiryAdapter.saveFeedback(new InquiryFeedback(a.getId(), true, null, NOW));

        LoadOfficeStatsPort.InquiryCounts inq = stats.countInquiries(officeId, FROM, TO);
        assertThat(inq).isEqualTo(new LoadOfficeStatsPort.InquiryCounts(3, 1, 2, 0));
        assertThat(stats.countFeedback(officeId, FROM, TO)).isEqualTo(new LoadOfficeStatsPort.FeedbackCounts(1, 0));
        assertThat(stats.findQuestions(officeId, InquiryOutcome.NO_GROUNDS, FROM, TO, 10)).containsExactly("can i work", "Can I work?");
        assertThat(stats.findQuestions(officeId, null, FROM, TO, 2)).hasSize(2);

        Long room1 = chatAdapter.save(ChatRoom.create(ChatRoomType.GROUP, "상담",
                List.of(new MemberId(studentId), new MemberId(staffId)))).getId().value();
        Long room2 = chatAdapter.save(ChatRoom.create(ChatRoomType.GROUP, "상담",
                List.of(new MemberId(studentId), new MemberId(staffId)))).getId().value();
        var ctx = new Consultation.SharedContext("Q", null, NoticeLang.EN, true);
        Consultation c1 = Consultation.open(officeId, studentId, null, "v1", ctx, room1, NOW);
        c1.assign(staffId, NOW.plusSeconds(60));
        c1.recordStaffReply(NOW.plusSeconds(600));
        c1.resolve(NOW.plusSeconds(7200));
        consultationAdapter.save(c1);
        consultationAdapter.save(Consultation.open(officeId, studentId, null, "v1", ctx, room2, NOW.plusSeconds(10)));

        assertThat(stats.countConsultations(officeId, FROM, TO)).isEqualTo(new LoadOfficeStatsPort.ConsultationCounts(1, 0, 1, 0));
        assertThat(stats.averageFirstReplyMinutes(officeId, FROM, TO)).isCloseTo(10.0, within(0.01));
        assertThat(stats.averageResolveHours(officeId, FROM, TO)).isCloseTo(2.0, within(0.01));
        assertThat(stats.averageFirstReplyMinutes(officeId, TO, TO.plusSeconds(1))).isNull();

        Notice soon = Notice.register(officeId, NoticeCategory.HOUSING, "곧 만료", "본문", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 5), staffId, NOW);
        soon.approve(String.valueOf(staffId), NOW, LocalDate.of(2027, 2, 1));
        noticeAdapter.save(soon);
        Notice later = Notice.register(officeId, NoticeCategory.HOUSING, "한참 뒤", "본문", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), staffId, NOW);
        later.approve(String.valueOf(staffId), NOW, LocalDate.of(2027, 2, 1));
        noticeAdapter.save(later);
        noticeAdapter.save(Notice.register(officeId, NoticeCategory.LIFE, "초안", "본문", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), staffId, NOW));

        assertThat(stats.countNotices(officeId, NoticeStatus.APPROVED)).isEqualTo(2);
        assertThat(stats.countNotices(officeId, NoticeStatus.DRAFT)).isEqualTo(1);
        assertThat(stats.findNoticesExpiringWithin(officeId, LocalDate.of(2027, 3, 2), 7))
                .extracting(Notice::getTitle).containsExactly("곧 만료");
    }
}
