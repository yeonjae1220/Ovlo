package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsultationEventListenerTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");

    @Mock LoadConsultationPort loadConsultationPort;
    @Mock SaveConsultationPort saveConsultationPort;
    @Mock me.yeonjae.ovlo.application.port.out.office.RevokeOfficeChatAccessPort chatAccess;

    @InjectMocks ConsultationEventListener listener;

    private static Consultation consultation() {
        Consultation c = Consultation.open(5L, 42L, null, "v1", new Consultation.SharedContext("q", null, NoticeLang.KO, true), 300L, NOW);
        c.assignId(700L);
        return c;
    }

    @Test
    @DisplayName("상담방에 담당자가 처음 답하면 firstStaffReplyAt 이 기록되고, 학생 메시지·두 번째 답변은 바꾸지 않는다")
    void recordsFirstStaffReply() {
        Consultation c = consultation();
        given(loadConsultationPort.findByChatRoomId(300L)).willReturn(Optional.of(c));

        listener.onMessageSent(new ChatMessageSentEvent(300L, 42L, NOW.plusSeconds(10)));
        assertThat(c.getFirstStaffReplyAt()).isNull();

        listener.onMessageSent(new ChatMessageSentEvent(300L, 11L, NOW.plusSeconds(20)));
        assertThat(c.getFirstStaffReplyAt()).isEqualTo(NOW.plusSeconds(20));
        verify(saveConsultationPort).save(c);

        listener.onMessageSent(new ChatMessageSentEvent(300L, 12L, NOW.plusSeconds(30)));
        assertThat(c.getFirstStaffReplyAt()).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    @DisplayName("상담방이 아닌 채팅 메시지는 무시한다")
    void ignoresOtherRooms() {
        given(loadConsultationPort.findByChatRoomId(999L)).willReturn(Optional.empty());

        listener.onMessageSent(new ChatMessageSentEvent(999L, 11L, NOW));

        verify(saveConsultationPort, never()).save(any());
    }

    @Test
    @DisplayName("담당자 제거는 상태와 참여자 수에 관계없이 권한 회수 포트에 전달된다")
    void removesStaffFromAllConsultations() {
        listener.onStaffRemoved(new OfficeStaffRemovedEvent(5L, 11L));
        verify(chatAccess).revoke(5L, 11L);
    }
}
