package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.result.TranscriptMessageResult;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.MessageTranslationPort;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.chat.model.MessageId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsultationTranscriptServiceTest {

    private static final Instant NOW = Instant.parse("2027-03-02T09:00:00Z");
    private static final Long OFFICE = 5L;
    private static final Long STUDENT = 42L;
    private static final Long STAFF = 11L;
    private static final Long ROOM = 300L;

    @Mock LoadConsultationPort loadConsultationPort;
    @Mock LoadChatPort loadChatPort;
    @Mock MessageTranslationPort messageTranslationPort;
    @Mock TranslationPort translationPort;

    private ConsultationTranscriptService sut;

    @BeforeEach
    void setUp() {
        sut = new ConsultationTranscriptService(loadConsultationPort, loadChatPort, messageTranslationPort, translationPort);
        Consultation c = Consultation.open(OFFICE, STUDENT, null, "v1",
                new Consultation.SharedContext("Visa?", null, NoticeLang.VI, true), ROOM, NOW);
        c.assignId(700L);
        given(loadConsultationPort.findById(700L)).willReturn(Optional.of(c));
        lenient().when(loadChatPort.findMessages(eq(new ChatRoomId(ROOM)), eq(0), anyInt())).thenReturn(List.of(
                Message.restore(new MessageId(1L), new MemberId(STUDENT), "Tôi có thể gia hạn visa không?", NOW),
                Message.restore(new MessageId(2L), new MemberId(STAFF), "출입국에 방문하셔야 합니다.", NOW.plusSeconds(60))));
    }

    @Test
    @DisplayName("학생이 보면 담당자 메시지(ko)만 학생 언어(vi)로 번역되고, 번역은 저장된다; 자기 메시지는 번역하지 않는다")
    void studentView() {
        given(messageTranslationPort.findTranslations(anyList(), eq(NoticeLang.VI))).willReturn(Map.of());
        given(translationPort.translate(any())).willReturn(new TranslationPort.Result("message", "Bạn phải đến văn phòng xuất nhập cảnh.", "m"));

        List<TranscriptMessageResult> out = sut.transcript(OFFICE, 700L, STUDENT, false, NoticeLang.VI, 100);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).fromStaff()).isFalse();
        assertThat(out.get(0).translatedText()).isNull();
        assertThat(out.get(1).fromStaff()).isTrue();
        assertThat(out.get(1).translatedText()).isEqualTo("Bạn phải đến văn phòng xuất nhập cảnh.");
        verify(messageTranslationPort).save(2L, NoticeLang.VI, "Bạn phải đến văn phòng xuất nhập cảnh.", "m");
        verify(translationPort).translate(any());
    }

    @Test
    @DisplayName("담당자가 ko 로 보면 학생 메시지(vi)만 번역하고, 저장된 번역이 있으면 다시 부르지 않는다")
    void staffViewUsesCache() {
        given(messageTranslationPort.findTranslations(anyList(), eq(NoticeLang.KO))).willReturn(Map.of(1L, "비자를 연장할 수 있나요?"));

        List<TranscriptMessageResult> out = sut.transcript(OFFICE, 700L, STAFF, true, NoticeLang.KO, 100);

        assertThat(out.get(0).translatedText()).isEqualTo("비자를 연장할 수 있나요?");
        assertThat(out.get(1).translatedText()).isNull();
        verify(translationPort, never()).translate(any());
    }

    @Test
    @DisplayName("번역이 실패하면 원문만 주고 translationFailed 를 켠다 (빈 값으로 치환하지 않는다)")
    void translationFailureIsVisible() {
        given(messageTranslationPort.findTranslations(anyList(), eq(NoticeLang.VI))).willReturn(Map.of());
        given(translationPort.translate(any())).willThrow(new IllegalStateException("provider down"));

        List<TranscriptMessageResult> out = sut.transcript(OFFICE, 700L, STUDENT, false, NoticeLang.VI, 100);

        assertThat(out.get(1).translatedText()).isNull();
        assertThat(out.get(1).translationFailed()).isTrue();
        assertThat(out.get(1).content()).isEqualTo("출입국에 방문하셔야 합니다.");
        verify(messageTranslationPort, never()).save(any(), any(), any(), any());
    }

    @Test
    @DisplayName("당사자가 아닌 학생은 FORBIDDEN, 다른 Office 는 NOT_FOUND")
    void access() {
        assertThatThrownBy(() -> sut.transcript(OFFICE, 700L, 43L, false, NoticeLang.KO, 10))
                .isInstanceOf(ConsultationException.class)
                .extracting(e -> ((ConsultationException) e).getErrorType())
                .isEqualTo(ConsultationException.ErrorType.FORBIDDEN);
        assertThatThrownBy(() -> sut.transcript(99L, 700L, STUDENT, false, NoticeLang.KO, 10))
                .isInstanceOf(ConsultationException.class);
    }
}
