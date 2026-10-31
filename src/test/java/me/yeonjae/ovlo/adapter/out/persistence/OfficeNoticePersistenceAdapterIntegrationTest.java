package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.TranslationStatus;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실 PostgreSQL + Flyway(V35) 로 공지·번역 영속과 조회 쿼리(필터·학생용·만료 대상)를 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficeNoticePersistenceAdapterIntegrationTest {

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

    private static final Instant NOW = Instant.parse("2027-02-01T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 2, 5);
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private OfficeNoticePersistenceAdapter adapter;
    @Autowired private OfficePersistenceAdapter officeAdapter;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long officeId;
    private Long staffId;

    @BeforeEach
    void setUp() {
        Long universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Notice Test University') RETURNING id", Long.class);
        int n = SEQ.incrementAndGet();
        Member m = Member.create("notice" + n, "담당자", "Seoul", new Email("notice" + n + "@example.com"),
                new Password("hashedPassword"), new UniversityId(universityId), new Major("IA", DegreeType.BACHELOR, 4));
        staffId = memberAdapter.save(m).getId().value();
        officeId = officeAdapter.save(Office.create(universityId, "국제처", NOW)).getId().value();
    }

    private Notice newNotice(NoticeCategory category, LocalDate from, LocalDate until) {
        return Notice.register(officeId, category, "공지 " + category, "2월 10일까지 신청", "https://u.ac.kr", List.of(3L, 4L),
                "2027-1", from, until, staffId, NOW);
    }

    @Test
    @DisplayName("공지와 번역을 저장하고 다시 읽으면 첨부·번역 상태·버전이 그대로다")
    void saveAndReload() {
        Notice n = newNotice(NoticeCategory.HOUSING, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15));
        n.approve(String.valueOf(staffId), NOW, LocalDate.of(2027, 2, 1));
        n.putTranslation(NoticeLang.EN, "Notice", "Apply by Feb 10", NOW);
        Notice saved = adapter.save(n);
        saved.reviewTranslation(NoticeLang.EN);
        adapter.save(saved);

        Notice reloaded = adapter.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(reloaded.getAttachmentMediaIds()).containsExactly(3L, 4L);
        assertThat(reloaded.getTranslation(NoticeLang.EN)).hasValueSatisfying(t -> {
            assertThat(t.getStatus()).isEqualTo(TranslationStatus.REVIEWED);
            assertThat(t.getSourceVersion()).isEqualTo(1);
        });
        assertThat(reloaded.isGroundingEligible(TODAY, NoticeLang.EN)).isTrue();
    }

    @Test
    @DisplayName("DB 제약: 만료일이 시작일보다 앞서는 행은 들어가지 않는다 (도메인을 우회해도 막힌다)")
    void dateCheckEnforcedByDb() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO office_notice (office_id, category, title, body, term, effective_from, expires_at, status, created_by)
                VALUES (?, 'OTHER', 't', 'b', '2027-1', DATE '2027-03-01', DATE '2027-02-01', 'DRAFT', ?)
                """, officeId, staffId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("담당자 필터 조회와 학생 조회(APPROVED + 오늘 기간 안)가 다른 상태·기간을 걸러낸다")
    void filteredQueries() {
        Notice draft = adapter.save(newNotice(NoticeCategory.HOUSING, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15)));
        Notice active = newNotice(NoticeCategory.VISA_STAY, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15));
        active.approve("x", NOW, LocalDate.of(2027, 2, 1));
        adapter.save(active);
        Notice future = newNotice(NoticeCategory.VISA_STAY, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 4, 1));
        future.approve("x", NOW, LocalDate.of(2027, 2, 1));
        adapter.save(future);

        assertThat(adapter.findByOffice(officeId, null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertThat(adapter.findByOffice(officeId, NoticeStatus.DRAFT, null, PageRequest.of(0, 10)).getContent())
                .extracting(Notice::getId).containsExactly(draft.getId());
        assertThat(adapter.findByOffice(officeId, null, NoticeCategory.VISA_STAY, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);

        assertThat(adapter.findActiveForStudent(officeId, null, TODAY)).extracting(Notice::getCategory)
                .containsExactly(NoticeCategory.VISA_STAY);
        assertThat(adapter.findActiveForStudent(officeId, NoticeCategory.HOUSING, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("만료 대상과 만료 임박 대상 id 를 날짜 기준으로 고른다")
    void expiryQueries() {
        Notice expired = newNotice(NoticeCategory.LIFE, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1));
        expired.approve("x", NOW, LocalDate.of(2027, 1, 15));
        Long expiredId = adapter.save(expired).getId();
        Notice soon = newNotice(NoticeCategory.LIFE, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 10));
        soon.approve("x", NOW, LocalDate.of(2027, 1, 15));
        Long soonId = adapter.save(soon).getId();
        Notice far = newNotice(NoticeCategory.LIFE, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 1));
        far.approve("x", NOW, LocalDate.of(2027, 1, 15));
        adapter.save(far);

        assertThat(adapter.findIdsToExpire(TODAY)).contains(expiredId).doesNotContain(soonId);
        assertThat(adapter.findIdsExpiringWithin(TODAY, 7)).contains(soonId).doesNotContain(expiredId);

        Notice soonReloaded = adapter.findById(soonId).orElseThrow();
        soonReloaded.markExpiryWarned(NOW);
        adapter.save(soonReloaded);
        assertThat(adapter.findIdsExpiringWithin(TODAY, 7)).doesNotContain(soonId);
        // 저장 전 객체를 다시 저장해도 새 행이 생기지 않는다(id 를 돌려받았다)
        assertThat(soon.getId()).isEqualTo(soonId);
    }
}
