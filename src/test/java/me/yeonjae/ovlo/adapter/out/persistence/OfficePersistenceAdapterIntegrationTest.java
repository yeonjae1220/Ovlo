package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실 PostgreSQL(Testcontainers) + Flyway(V34) 로 Office 애그리거트 영속을 검증한다.
 * - V34 SQL 이 실제로 적용되는가, 대학당 Office 1개(uq_office_university)가 DB 에서도 강제되는가
 * - 담당자 상태 전이(INVITED→ACTIVE→REMOVED→재초대)가 같은 행을 재사용해 저장되는가
 * - 토큰 발급 경로의 소속 프로젝션이 정지된 Office 를 제외하는가
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficePersistenceAdapterIntegrationTest {

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

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private OfficePersistenceAdapter adapter;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long universityId;

    @BeforeEach
    void setUp() {
        universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Office Test University') RETURNING id", Long.class);
    }

    private Long newMember() {
        int n = SEQ.incrementAndGet();
        Member m = Member.create("officer" + n, "담당자", "Seoul",
                new Email("officer" + n + "@example.com"), new Password("hashedPassword"),
                new UniversityId(universityId), new Major("International Affairs", DegreeType.BACHELOR, 4));
        return memberAdapter.save(m).getId().value();
    }

    @Test
    @DisplayName("Office 와 담당자를 저장하고 다시 읽으면 상태·시각·역할이 그대로다")
    void saveAndReload() {
        Long admin = newMember();
        Office office = Office.create(universityId, "국제처", NOW);
        office.invite(admin, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);

        Office saved = adapter.save(office);
        saved.accept(admin, NOW.plusSeconds(10));
        adapter.save(saved);

        Office reloaded = adapter.findById(saved.getId().value()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OfficeStatus.PILOT);
        assertThat(reloaded.getStaff()).hasSize(1);
        assertThat(reloaded.findStaff(admin).orElseThrow().getStatus()).isEqualTo(OfficeStaffStatus.ACTIVE);
        assertThat(reloaded.findStaff(admin).orElseThrow().getAcceptedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(adapter.findByUniversityId(universityId)).isPresent();
    }

    @Test
    @DisplayName("대학당 Office 1개는 DB 유니크 제약(uq_office_university)으로도 막힌다 (GLOBAL-PIT-184: catch 해 멱등 처리하지 않음)")
    void oneOfficePerUniversityEnforcedByDb() {
        adapter.save(Office.create(universityId, "첫 번째", NOW));

        assertThatThrownBy(() -> adapter.save(Office.create(universityId, "두 번째", NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("제거 뒤 재초대는 같은 담당자 행을 INVITED 로 되돌린다 (행이 늘지 않는다)")
    void reinviteReusesRow() {
        Long admin = newMember();
        Long staff = newMember();
        Office office = Office.create(universityId, "국제처", NOW);
        office.invite(admin, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);
        office.accept(admin, NOW);
        office.invite(staff, OfficeRole.OFFICE_STAFF, String.valueOf(admin), NOW);
        office.accept(staff, NOW);
        Office saved = adapter.save(office);

        saved.remove(staff, NOW.plusSeconds(5));
        adapter.save(saved);
        Office again = adapter.findById(saved.getId().value()).orElseThrow();
        again.invite(staff, OfficeRole.OFFICE_ADMIN, String.valueOf(admin), NOW.plusSeconds(10));
        adapter.save(again);

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM office_staff WHERE office_id = ?", Integer.class,
                saved.getId().value());
        assertThat(rows).isEqualTo(2);
        Office reloaded = adapter.findById(saved.getId().value()).orElseThrow();
        assertThat(reloaded.findStaff(staff).orElseThrow().getStatus()).isEqualTo(OfficeStaffStatus.INVITED);
        assertThat(reloaded.findStaff(staff).orElseThrow().getRole()).isEqualTo(OfficeRole.OFFICE_ADMIN);
        assertThat(adapter.findOfficeWithInvitationFor(staff)).map(o -> o.getId().value()).contains(saved.getId().value());
    }

    @Test
    @DisplayName("소속 프로젝션은 ACTIVE 담당자만 주고, Office 가 정지되면 비어 있다")
    void activeMembershipProjection() {
        Long admin = newMember();
        Long invitedOnly = newMember();
        Office office = Office.create(universityId, "국제처", NOW);
        office.invite(admin, OfficeRole.OFFICE_ADMIN, "admin@ovlo.test", NOW);
        office.accept(admin, NOW);
        office.invite(invitedOnly, OfficeRole.OFFICE_STAFF, String.valueOf(admin), NOW);
        Office saved = adapter.save(office);

        Optional<OfficeMembership> membership = adapter.findActiveMembership(new MemberId(admin));
        assertThat(membership).contains(new OfficeMembership(saved.getId().value(), OfficeRole.OFFICE_ADMIN));
        assertThat(adapter.findActiveMembership(new MemberId(invitedOnly))).isEmpty();

        saved.suspend();
        adapter.save(saved);
        assertThat(adapter.findActiveMembership(new MemberId(admin))).isEmpty();
    }
}
