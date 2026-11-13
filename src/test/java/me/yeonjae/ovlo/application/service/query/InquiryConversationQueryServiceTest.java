package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InquiryConversationQueryServiceTest {
    LoadInquiryPort inquiries = mock(LoadInquiryPort.class);
    LoadConsultationPort consultations = mock(LoadConsultationPort.class);
    StudentOfficeAccessPolicy access = mock(StudentOfficeAccessPolicy.class);
    InquiryConversationQueryService service = new InquiryConversationQueryService(inquiries, mock(LoadNoticePort.class), consultations, access, mock(NoticeKnowledgePort.class), me.yeonjae.ovlo.support.NoticeFixtures.governance(mock(LoadNoticePort.class)),java.time.Clock.systemUTC());

    Inquiry turn(long id, long office, long member) {
        Inquiry i = Inquiry.noGrounds(office, member, NoticeLang.KO, "question " + id, List.of(), Instant.now(), 0, null);
        i.assignId(id);
        when(inquiries.findById(id)).thenReturn(Optional.of(i));
        return i;
    }

    @Test void reloadRecoversFollowUpAndHandoffFromEarlierBookmark() {
        Inquiry first = turn(10, 1, 2);
        Inquiry last = Inquiry.noGrounds(1L, 2L, NoticeLang.KO, "follow up", List.of(), Instant.now(), 0, null).following(first);
        last.assignId(11L);
        last.markEscalated();
        when(inquiries.findByPreviousInquiryId(10L)).thenReturn(Optional.of(last));
        Consultation consultation = Consultation.open(1L, 2L, 11L, "v2",
                new Consultation.SharedContext("follow up", null, NoticeLang.KO, true), 90L, Instant.now());
        consultation.assignId(30L);
        when(consultations.findFirstByInquiryId(11L)).thenReturn(Optional.of(consultation));
        var result = service.conversation(1L, 2L, 10L);
        assertThat(result.turns()).hasSize(2);
        assertThat(result.turns().getLast().result().inquiryId()).isEqualTo(11L);
        assertThat(result.consultationId()).isEqualTo(30L);
        assertThat(result.canContinue()).isFalse();
        verify(access).assertMember(2L, 1L);
    }

    @Test void cannotReadAnotherStudentOrSchoolHistory() {
        turn(10, 1, 2);
        assertThatThrownBy(() -> service.conversation(1L, 3L, 10L)).hasMessageContaining("찾을 수 없습니다");
        assertThatThrownBy(() -> service.conversation(9L, 2L, 10L)).hasMessageContaining("찾을 수 없습니다");
        verifyNoInteractions(consultations);
    }

    @Test void reloadingRejectedTurnNeverOffersHandoff() {
        Inquiry i=Inquiry.outOfScope(1L,2L,NoticeLang.KO,"write code",Instant.now(),0,"sonnet");
        i.assignId(10L);when(inquiries.findById(10L)).thenReturn(Optional.of(i));
        var result=service.conversation(1L,2L,10L);
        assertThat(result.turns().getFirst().result().outcome()).isEqualTo(InquiryOutcome.OUT_OF_SCOPE);
        assertThat(result.turns().getFirst().result().canEscalate()).isFalse();
        assertThat(result.canContinue()).isTrue();
        assertThatThrownBy(i::markEscalated).hasMessageContaining("범위");
    }
}
