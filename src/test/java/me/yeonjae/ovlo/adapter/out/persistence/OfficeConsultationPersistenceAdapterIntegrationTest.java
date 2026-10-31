package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실 PostgreSQL + Flyway(V37) 로 상담·메시지 번역 영속과 채팅방 참여자 변경 저장을 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OfficeConsultationPersistenceAdapterIntegrationTest {

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
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private OfficeConsultationPersistenceAdapter adapter;
    @Autowired private OfficePersistenceAdapter officeAdapter;
    @Autowired private ChatPersistenceAdapter chatAdapter;
    @Autowired private MemberPersistenceAdapter memberAdapter;
    @Autowired private JdbcTemplate jdbc;

    private Long officeId;
    private Long studentId;
    private Long staffId;

    @BeforeEach
    void setUp() {
        Long universityId = jdbc.queryForObject(
                "INSERT INTO global_universities (name_en) VALUES ('Consultation Test University') RETURNING id", Long.class);
        studentId = newMember(universityId);
        staffId = newMember(universityId);
        officeId = officeAdapter.save(Office.create(universityId, "국제처", NOW)).getId().value();
    }

    private Long newMember(Long universityId) {
        int n = SEQ.incrementAndGet();
        Member m = Member.create("cons" + n, "이름", "Seoul", new Email("cons" + n + "@example.com"),
                new Password("hashedPassword"), new UniversityId(universityId), new Major("CS", DegreeType.BACHELOR, 1));
        return memberAdapter.save(m).getId().value();
    }

    private Long newRoom() {
        return chatAdapter.save(ChatRoom.create(ChatRoomType.GROUP, "상담",
                List.of(new MemberId(studentId), new MemberId(staffId)))).getId().value();
    }

    @Test
    @DisplayName("상담을 저장·전이하고 Office 받은함·내 상담·자동 종료 대상으로 읽는다; 채팅방당 상담 1개")
    void saveAndQuery() {
        Long roomId = newRoom();
        Consultation c = adapter.save(Consultation.open(officeId, studentId, null, "v1",
                new Consultation.SharedContext("비자 연장?", "답변 [1]", NoticeLang.VI, true), roomId, NOW));

        Consultation reloaded = adapter.findById(c.getId()).orElseThrow();
        assertThat(reloaded.getConsentText()).contains("국제처 담당자에게 전달");
        assertThat(reloaded.getSharedContext().lang()).isEqualTo(NoticeLang.VI);
        assertThat(adapter.findByChatRoomId(roomId)).isPresent();

        reloaded.assign(staffId, NOW.plusSeconds(60));
        reloaded.resolve(NOW.plusSeconds(120));
        adapter.save(reloaded);

        assertThat(adapter.findByOffice(officeId, ConsultationStatus.RESOLVED, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(adapter.findByOffice(officeId, ConsultationStatus.OPEN, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(adapter.findByMember(officeId, studentId)).extracting(Consultation::getId).containsExactly(c.getId());
        assertThat(adapter.findActiveByOffice(officeId)).hasSize(1);
        // 서비스는 cutoff = now - 7일 로 부른다: resolvedAt <= cutoff 인 것만 대상
        Instant resolvedAt = NOW.plusSeconds(120);
        assertThat(adapter.findIdsResolvedBefore(resolvedAt)).contains(c.getId());
        assertThat(adapter.findIdsResolvedBefore(resolvedAt.minus(Duration.ofSeconds(1)))).doesNotContain(c.getId());

        assertThatThrownBy(() -> adapter.save(Consultation.open(officeId, studentId, null, "v1",
                new Consultation.SharedContext("q2", null, NoticeLang.KO, true), roomId, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("메시지 번역은 (message, lang) 당 1건이고 덮어쓴다; 채팅방 참여자 변경이 저장된다")
    void translationsAndParticipants() {
        Long roomId = newRoom();
        Long messageId = chatAdapter.saveMessage(roomId, studentId, "Tôi có thể gia hạn visa không?").getId().value();

        adapter.save(messageId, NoticeLang.KO, "비자를 연장할 수 있나요?", "m1");
        adapter.save(messageId, NoticeLang.KO, "비자 연장이 가능한가요?", "m2");
        Map<Long, String> ko = adapter.findTranslations(List.of(messageId), NoticeLang.KO);
        assertThat(ko).containsEntry(messageId, "비자 연장이 가능한가요?");
        assertThat(adapter.findTranslations(List.of(messageId), NoticeLang.EN)).isEmpty();
        assertThat(adapter.findTranslations(List.of(), NoticeLang.KO)).isEmpty();

        Long extraStaff = newMember(jdbc.queryForObject("SELECT global_univ_id FROM office WHERE id = ?", Long.class, officeId));
        ChatRoom room = chatAdapter.findById(new me.yeonjae.ovlo.domain.chat.model.ChatRoomId(roomId)).orElseThrow();
        room.addParticipant(new MemberId(extraStaff));
        chatAdapter.save(room);
        ChatRoom bigger = chatAdapter.findById(new me.yeonjae.ovlo.domain.chat.model.ChatRoomId(roomId)).orElseThrow();
        assertThat(bigger.getParticipants()).hasSize(3);

        bigger.removeParticipant(new MemberId(staffId));
        chatAdapter.save(bigger);
        assertThat(chatAdapter.isMember(new me.yeonjae.ovlo.domain.chat.model.ChatRoomId(roomId), new MemberId(staffId))).isFalse();
        assertThat(chatAdapter.isMember(new me.yeonjae.ovlo.domain.chat.model.ChatRoomId(roomId), new MemberId(extraStaff))).isTrue();
    }
}
