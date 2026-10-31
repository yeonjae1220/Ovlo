package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeTranslationServiceTest {

    private static final Instant NOW = Instant.parse("2027-02-01T09:00:00Z");
    private static final Long NOTICE = 77L;

    @Mock LoadNoticePort loadNoticePort;
    @Mock SaveNoticePort saveNoticePort;
    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadMemberPort loadMemberPort;
    @Mock TranslationPort translationPort;
    @Mock OfficeNotificationPort notificationPort;

    private NoticeTranslationService sut;
    private Notice notice;

    @BeforeEach
    void setUp() {
        sut = new NoticeTranslationService(loadNoticePort, saveNoticePort, loadOfficePort, loadMemberPort,
                translationPort, new NoticeTranslationGate(), notificationPort, Clock.fixed(NOW, ZoneOffset.UTC));
        notice = Notice.register(5L, NoticeCategory.HOUSING, "기숙사 신청", "신청은 2월 10일까지입니다.", null, List.of(),
                "2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15), 11L, NOW);
        notice.assignId(NOTICE);
        notice.approve("11", NOW, LocalDate.of(2027, 2, 1));
        given(loadNoticePort.findById(NOTICE)).willReturn(Optional.of(notice));
        lenient().when(saveNoticePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Office office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(5L));
        lenient().when(loadOfficePort.findById(5L)).thenReturn(Optional.of(office));
        lenient().when(loadMemberPort.findAllByIds(anyList())).thenReturn(List.of());
    }

    @Test
    @DisplayName("6개 언어가 모두 게이트를 통과하면 전부 저장하고 알림은 없다")
    void translatesAllTargets() {
        given(translationPort.translate(any())).willAnswer(inv -> {
            TranslationPort.Request r = inv.getArgument(0);
            return new TranslationPort.Result("[" + r.to().code() + "] " + r.title(), "[" + r.to().code() + "] " + r.body(), "m");
        });

        TranslateNoticeUseCase.Outcome outcome = sut.translateMissing(NOTICE);

        assertThat(outcome.generated()).hasSize(6).doesNotContain(NoticeLang.KO);
        assertThat(outcome.failed()).isEmpty();
        assertThat(notice.isGroundingEligible(LocalDate.of(2027, 2, 5), NoticeLang.VI)).isTrue();
        verify(saveNoticePort).save(notice);
        verify(notificationPort, never()).sendNoticeTranslationFailed(any(), any(), any(), any());
    }

    @Test
    @DisplayName("숫자를 바꾼 언어와 예외를 던진 언어는 저장하지 않고 담당자에게 알린다. 나머지는 저장된다")
    void partialFailureIsIsolated() {
        given(translationPort.translate(any())).willAnswer(inv -> {
            TranslationPort.Request r = inv.getArgument(0);
            if (r.to() == NoticeLang.JA) return new TranslationPort.Result("申請", "2月12日まで", "m"); // 숫자 변형
            if (r.to() == NoticeLang.DE) throw new IllegalStateException("provider down");
            return new TranslationPort.Result("[" + r.to().code() + "] " + r.title(), "[" + r.to().code() + "] " + r.body(), "m");
        });

        TranslateNoticeUseCase.Outcome outcome = sut.translateMissing(NOTICE);

        assertThat(outcome.failed()).containsExactlyInAnyOrder(NoticeLang.JA, NoticeLang.DE);
        assertThat(outcome.generated()).containsExactlyInAnyOrder(NoticeLang.EN, NoticeLang.ZH, NoticeLang.FR, NoticeLang.VI);
        assertThat(notice.getTranslation(NoticeLang.JA)).isEmpty();
        assertThat(notice.getTranslation(NoticeLang.EN)).isPresent();
        verify(saveNoticePort).save(notice);
        verify(notificationPort).sendNoticeTranslationFailed(anyList(), eq("국제처"), eq("기숙사 신청"), eq(List.of("ja", "de")));
    }

    @Test
    @DisplayName("이미 최신 번역이 있는 언어는 다시 번역하지 않는다")
    void skipsFreshTranslations() {
        for (NoticeLang l : NoticeLang.ALL) {
            if (l != NoticeLang.KO) notice.putTranslation(l, "[" + l.code() + "] t", "[" + l.code() + "] 2월 10일", NOW);
        }

        TranslateNoticeUseCase.Outcome outcome = sut.translateMissing(NOTICE);

        assertThat(outcome.generated()).isEmpty();
        assertThat(outcome.failed()).isEmpty();
        verify(translationPort, never()).translate(any());
        verify(saveNoticePort, never()).save(any());
    }
}
