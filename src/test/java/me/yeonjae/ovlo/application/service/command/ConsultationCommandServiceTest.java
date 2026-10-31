package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ConsultationActionCommand;
import me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveInquiryPort;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.chat.model.MessageId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsultationCommandServiceTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final Long OFFICE = 5L;
    private static final Long STUDENT = 42L;
    private static final Long STAFF_A = 11L;
    private static final Long STAFF_B = 12L;
    private static final Long ROOM = 300L;

    @Mock StudentOfficeAccessPolicy accessPolicy;
    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadConsultationPort loadConsultationPort;
    @Mock SaveConsultationPort saveConsultationPort;
    @Mock LoadInquiryPort loadInquiryPort;
    @Mock SaveInquiryPort saveInquiryPort;
    @Mock LoadChatPort loadChatPort;
    @Mock SaveChatPort saveChatPort;
    @Mock LoadMemberPort loadMemberPort;
    @Mock OfficeNotificationPort notificationPort;
    @Mock TransactionTemplate tx;

    private ConsultationCommandService sut;
    private Office office;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sut = new ConsultationCommandService(accessPolicy, loadOfficePort, loadConsultationPort, saveConsultationPort,
                loadInquiryPort, saveInquiryPort, loadChatPort, saveChatPort, loadMemberPort, notificationPort, tx,
                Clock.fixed(NOW, ZoneOffset.UTC));
        office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE));
        office.invite(STAFF_A, OfficeRole.OFFICE_ADMIN, "admin", NOW);
        office.accept(STAFF_A, NOW);
        office.invite(STAFF_B, OfficeRole.OFFICE_STAFF, "11", NOW);
        office.accept(STAFF_B, NOW);
        lenient().when(accessPolicy.assertMember(STUDENT, OFFICE)).thenReturn(office);
        lenient().when(loadOfficePort.findById(OFFICE)).thenReturn(Optional.of(office));
        lenient().when(saveConsultationPort.save(any())).thenAnswer(inv -> {
            Consultation c = inv.getArgument(0);
            if (c.getId() == null) c.assignId(700L);
            return c;
        });
        lenient().when(saveChatPort.save(any(ChatRoom.class))).thenAnswer(inv -> {
            ChatRoom r = inv.getArgument(0);
            return ChatRoom.restore(new ChatRoomId(ROOM), r.getType(), r.getName(), r.getParticipants(), r.getMessages());
        });
        lenient().when(saveChatPort.saveMessage(anyLong(), anyLong(), anyString()))
                .thenAnswer(inv -> Message.restore(new MessageId(1L), new MemberId(inv.getArgument(1)), inv.getArgument(2), NOW));
        lenient().when(loadMemberPort.findAllByIds(anyList())).thenReturn(List.of());
        lenient().when(loadMemberPort.findById(any())).thenReturn(Optional.empty());
        lenient().when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
    }

    private Consultation stored(ConsultationStatus status) {
        Consultation c = Consultation.open(OFFICE, STUDENT, null, "v1",
                new Consultation.SharedContext("q", null, NoticeLang.VI, true), ROOM, NOW);
        c.assignId(700L);
        if (status == ConsultationStatus.IN_PROGRESS || status == ConsultationStatus.RESOLVED) c.assign(STAFF_A, NOW);
        if (status == ConsultationStatus.RESOLVED) c.resolve(NOW);
        given(loadConsultationPort.findById(700L)).willReturn(Optional.of(c));
        return c;
    }

    @Test
    @DisplayName("문의를 전달하면 학생+활동 담당자 GROUP 방을 만들고, 질문을 첫 메시지로 넣고, 문의를 ESCALATED 로 표시하고, 담당자에게 알린다")
    void openFromInquiry() {
        Inquiry inquiry = Inquiry.answered(OFFICE, STUDENT, NoticeLang.VI, "Visa gia hạn?", "Liên hệ [1]", List.of(77L), List.of(77L), NOW, 1, "m");
        inquiry.assignId(900L);
        given(loadInquiryPort.findById(900L)).willReturn(Optional.of(inquiry));

        ConsultationResult r = sut.open(new OpenConsultationCommand(OFFICE, STUDENT, 900L, "v1", null));

        assertThat(r.status()).isEqualTo(ConsultationStatus.OPEN);
        assertThat(r.chatRoomId()).isEqualTo(ROOM);
        assertThat(r.question()).isEqualTo("Visa gia hạn?");
        assertThat(r.answerText()).isEqualTo("Liên hệ [1]");
        assertThat(r.lang()).isEqualTo(NoticeLang.VI);

        ArgumentCaptor<ChatRoom> room = ArgumentCaptor.forClass(ChatRoom.class);
        verify(saveChatPort).save(room.capture());
        assertThat(room.getValue().getType()).isEqualTo(ChatRoomType.GROUP);
        assertThat(room.getValue().getParticipants()).containsExactly(new MemberId(STUDENT), new MemberId(STAFF_A), new MemberId(STAFF_B));
        verify(saveChatPort).saveMessage(ROOM, STUDENT, "Visa gia hạn?");
        assertThat(inquiry.getOutcome()).isEqualTo(InquiryOutcome.ESCALATED);
        verify(saveInquiryPort).save(inquiry);
        verify(notificationPort).sendConsultationOpened(anyList(), eq("국제처"), eq("Visa gia hạn?"));
    }

    @Test
    @DisplayName("동의 버전이 없으면 CONSENT_REQUIRED 이고 아무것도 만들지 않는다; 활동 담당자가 없으면 BAD_REQUEST")
    void consentAndStaffRequired() {
        assertThatThrownBy(() -> sut.open(new OpenConsultationCommand(OFFICE, STUDENT, null, "", "도와주세요")))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.CONSENT_REQUIRED);
        verify(saveConsultationPort, never()).save(any());
        // 방은 만들어졌을 수 있으나 상담은 저장되지 않는다 — 트랜잭션이 되돌린다

        Office noStaff = Office.create(267L, "빈 국제처", NOW);
        noStaff.assignId(new OfficeId(6L));
        given(accessPolicy.assertMember(STUDENT, 6L)).willReturn(noStaff);
        assertThatThrownBy(() -> sut.open(new OpenConsultationCommand(6L, STUDENT, null, "v1", "도와주세요")))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.BAD_REQUEST);
    }

    @Test
    @DisplayName("다른 학생의 문의는 FORBIDDEN, 문의 없이 메시지도 없으면 BAD_REQUEST")
    void inquiryOwnershipAndMessage() {
        Inquiry other = Inquiry.noGrounds(OFFICE, 43L, NoticeLang.KO, "q", List.of(), NOW, 1, null);
        other.assignId(901L);
        given(loadInquiryPort.findById(901L)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> sut.open(new OpenConsultationCommand(OFFICE, STUDENT, 901L, "v1", null)))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> sut.open(new OpenConsultationCommand(OFFICE, STUDENT, null, "v1", "  ")))
                .isInstanceOf(ConsultationException.class);
    }

    @Test
    @DisplayName("맡기: 활동 담당자만, 방에 없으면 넣는다; 담당자가 아니면 FORBIDDEN")
    void assign() {
        stored(ConsultationStatus.OPEN);
        ChatRoom room = ChatRoom.restore(new ChatRoomId(ROOM), ChatRoomType.GROUP, "상담",
                List.of(new MemberId(STUDENT), new MemberId(STAFF_B)), List.of());
        given(loadChatPort.findById(new ChatRoomId(ROOM))).willReturn(Optional.of(room));

        ConsultationResult r = sut.assign(new ConsultationActionCommand(OFFICE, 700L, STAFF_A));

        assertThat(r.status()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(r.assignedStaffMemberId()).isEqualTo(STAFF_A);
        assertThat(room.hasParticipant(new MemberId(STAFF_A))).isTrue();
        verify(saveChatPort).save(room);

        assertThatThrownBy(() -> sut.assign(new ConsultationActionCommand(OFFICE, 700L, 99L)))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.FORBIDDEN);
    }

    @Test
    @DisplayName("종료는 학생 본인만; 다른 Office 의 상담은 NOT_FOUND")
    void closeOwnership() {
        stored(ConsultationStatus.IN_PROGRESS);

        assertThatThrownBy(() -> sut.close(new ConsultationActionCommand(OFFICE, 700L, STAFF_A)))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.FORBIDDEN);
        assertThat(sut.close(new ConsultationActionCommand(OFFICE, 700L, STUDENT)).status()).isEqualTo(ConsultationStatus.CLOSED);
        assertThatThrownBy(() -> sut.resolve(new ConsultationActionCommand(99L, 700L, STAFF_A)))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("자동 종료: RESOLVED 뒤 7일 지난 것만, 건별 트랜잭션")
    void closeDue() {
        Consultation old = Consultation.open(OFFICE, STUDENT, null, "v1", new Consultation.SharedContext("q", null, NoticeLang.KO, true), ROOM,
                NOW.minus(Duration.ofDays(10)));
        old.assignId(701L);
        old.assign(STAFF_A, NOW.minus(Duration.ofDays(9)));
        old.resolve(NOW.minus(Duration.ofDays(8)));
        Consultation recent = Consultation.open(OFFICE, STUDENT, null, "v1", new Consultation.SharedContext("q", null, NoticeLang.KO, true), 301L, NOW);
        recent.assignId(702L);
        recent.assign(STAFF_A, NOW);
        recent.resolve(NOW.minus(Duration.ofDays(2)));
        given(loadConsultationPort.findIdsResolvedBefore(NOW.minus(Consultation.AUTO_CLOSE_AFTER_RESOLVED))).willReturn(List.of(701L, 702L));
        given(loadConsultationPort.findById(701L)).willReturn(Optional.of(old));
        given(loadConsultationPort.findById(702L)).willReturn(Optional.of(recent));

        assertThat(sut.closeDue()).isEqualTo(1);
        assertThat(old.getStatus()).isEqualTo(ConsultationStatus.CLOSED);
        assertThat(recent.getStatus()).isEqualTo(ConsultationStatus.RESOLVED);
    }
}
