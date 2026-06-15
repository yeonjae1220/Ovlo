package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.VerificationCredentialJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.VerificationCredentialJpaRepository;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationStatus;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실 PostgreSQL(Testcontainers) + Flyway(V22 부분유니크 인덱스, V24/V25)로 검증.
 * H2는 부분 유니크 인덱스/식 인덱스를 생성하지 않으므로 이 불변식은 PG로만 검증 가능.
 *
 * <p>검증 대상:
 * <ul>
 *   <li>ADMIN_VERIFIED와 SCHOOL_EMAIL이 UNIQUE(member_id,type)로 공존</li>
 *   <li>무이메일(null) ADMIN_VERIFIED가 여러 회원에 다건 공존(lower(null) 식인덱스 제외)</li>
 *   <li>활성 이메일 전역 유일성(uq_vc_verified_email_active)은 그대로 강제</li>
 *   <li>revokeByIdAndMemberId가 소유범위를 한정하고 취소 감사필드를 기록</li>
 * </ul>
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class VerificationCredentialPersistenceAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Flyway가 스키마를 온전히 소유 — V22 부분유니크/식인덱스, V24/V25 적용
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private VerificationCredentialPersistenceAdapter adapter;

    @Autowired
    private VerificationCredentialJpaRepository repository;

    @Autowired
    private MemberPersistenceAdapter memberAdapter;

    @Autowired
    private JdbcTemplate jdbc;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Long universityId;

    @BeforeEach
    void setUp() {
        universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Test University') RETURNING id",
                Long.class);
    }

    private Long newMember() {
        int n = SEQ.incrementAndGet();
        Member m = Member.create(
                "tester" + n, "테스터", "Seoul",
                new Email("tester" + n + "@example.com"),
                new Password("hashedPassword"),
                new UniversityId(universityId),
                new Major("Computer Science", DegreeType.BACHELOR, 3));
        return memberAdapter.save(m).getId().value();
    }

    @Test
    @DisplayName("ADMIN_VERIFIED와 SCHOOL_EMAIL은 같은 회원에 공존한다 (UNIQUE(member_id,type))")
    void adminVerifiedAndSchoolEmailCoexist() {
        Long memberId = newMember();
        adapter.save(VerificationCredential.issue(
                memberId, VerificationType.SCHOOL_EMAIL, universityId, "tester@univ.edu", Instant.now()));
        adapter.save(VerificationCredential.issueManual(
                memberId, universityId, null, "admin@ovlo.me", "서류 확인", Instant.now()));

        List<VerificationCredential> creds = adapter.findByMemberId(memberId);

        assertThat(creds).hasSize(2);
        assertThat(creds).extracting(VerificationCredential::getType)
                .containsExactlyInAnyOrder(VerificationType.SCHOOL_EMAIL, VerificationType.ADMIN_VERIFIED);
    }

    @Test
    @DisplayName("무이메일 ADMIN_VERIFIED는 여러 회원에 다건 공존한다 (lower(null) 식인덱스 제외)")
    void multipleNullEmailAdminVerifiedCoexistAcrossMembers() {
        Long m1 = newMember();
        Long m2 = newMember();

        adapter.save(VerificationCredential.issueManual(m1, universityId, null, "admin@ovlo.me", null, Instant.now()));
        adapter.save(VerificationCredential.issueManual(m2, universityId, null, "admin@ovlo.me", null, Instant.now()));

        assertThat(adapter.findByMemberId(m1)).hasSize(1);
        assertThat(adapter.findByMemberId(m2)).hasSize(1);
    }

    @Test
    @DisplayName("활성 이메일 전역 유일성은 그대로 강제된다 (타 회원 동일 이메일 거부)")
    void duplicateActiveEmailAcrossMembersRejected() {
        Long m1 = newMember();
        Long m2 = newMember();
        adapter.save(VerificationCredential.issue(
                m1, VerificationType.SCHOOL_EMAIL, universityId, "dup@univ.edu", Instant.now()));

        assertThatThrownBy(() -> adapter.save(VerificationCredential.issue(
                m2, VerificationType.SCHOOL_EMAIL, universityId, "dup@univ.edu", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("revoke는 소유 회원만 취소하고, 취소자/취소시각을 기록한다")
    void revokeScopedByMemberRecordsAudit() {
        Long owner = newMember();
        Long other = newMember();
        VerificationCredential saved = adapter.save(VerificationCredential.issueManual(
                owner, universityId, null, "admin@ovlo.me", "서류", Instant.now()));
        Long credentialId = saved.getId().value();
        Instant revokedAt = Instant.parse("2026-06-15T00:00:00Z");

        // 소유자 불일치 → 0행, 상태 변화 없음
        int wrongOwner = adapter.revokeByIdAndMemberId(credentialId, other, "admin@ovlo.me", revokedAt);
        assertThat(wrongOwner).isZero();
        assertThat(repository.findById(credentialId)).get()
                .extracting(VerificationCredentialJpaEntity::getStatus)
                .isEqualTo(VerificationStatus.VERIFIED.name());

        // 소유자 일치 → 1행, EXPIRED + 감사필드 기록
        int affected = adapter.revokeByIdAndMemberId(credentialId, owner, "admin@ovlo.me", revokedAt);
        assertThat(affected).isEqualTo(1);

        VerificationCredentialJpaEntity revoked = repository.findById(credentialId).orElseThrow();
        assertThat(revoked.getStatus()).isEqualTo(VerificationStatus.EXPIRED.name());
        assertThat(revoked.getRevokedBy()).isEqualTo("admin@ovlo.me");
        assertThat(revoked.getRevokedAt()).isEqualTo(revokedAt);
    }
}
