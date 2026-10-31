package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.application.service.office.NoticeApprovedEvent;
import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoticeCommandServiceTest {

    private static final Instant NOW = Instant.parse("2027-02-01T09:00:00Z");
    private static final Long OFFICE = 5L;
    private static final Long NOTICE = 77L;

    @Mock LoadNoticePort loadNoticePort;
    @Mock SaveNoticePort saveNoticePort;
    @Mock LoadOfficePort loadOfficePort;
    @Mock TranslateNoticeUseCase translateNoticeUseCase;
    @Mock ApplicationEventPublisher eventPublisher;

    private NoticeCommandService sut;
    private Office office;

    @BeforeEach
    void setUp() {
        sut = new NoticeCommandService(loadNoticePort, saveNoticePort, loadOfficePort, translateNoticeUseCase,
                eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
        office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(OFFICE));
        lenient().when(loadOfficePort.findById(OFFICE)).thenReturn(Optional.of(office));
        lenient().when(saveNoticePort.save(any(Notice.class))).thenAnswer(inv -> {
            Notice n = inv.getArgument(0);
            if (n.getId() == null) n.assignId(NOTICE);
            return n;
        });
    }

    private static RegisterNoticeCommand registerCommand() {
        return new RegisterNoticeCommand(OFFICE, NoticeCategory.HOUSING, "기숙사 신청", "2월 10일까지", "https://u.ac.kr",
                List.of(), "2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15), 11L);
    }

    private Notice draftInStore() {
        Notice n = Notice.register(OFFICE, NoticeCategory.HOUSING, "기숙사 신청", "2월 10일까지", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15), 11L, NOW);
        n.assignId(NOTICE);
        given(loadNoticePort.findById(NOTICE)).willReturn(Optional.of(n));
        return n;
    }

    @Test
    @DisplayName("등록하면 DRAFT 로 저장되고 7개 언어 중 원문을 뺀 6개가 missingLangs 다")
    void registersDraft() {
        NoticeResult result = sut.register(registerCommand());

        assertThat(result.id()).isEqualTo(NOTICE);
        assertThat(result.status()).isEqualTo(NoticeStatus.DRAFT);
        assertThat(result.missingLangs()).hasSize(6).doesNotContain(NoticeLang.KO);
        assertThat(result.activeToday()).isFalse();
    }

    @Test
    @DisplayName("정지된 Office 에는 등록할 수 없다")
    void suspendedOfficeRejects() {
        office.suspend();

        assertThatThrownBy(() -> sut.register(registerCommand()))
                .isInstanceOf(OfficeException.class)
                .extracting(e -> ((OfficeException) e).getErrorType())
                .isEqualTo(OfficeException.ErrorType.SUSPENDED);
        verify(saveNoticePort, never()).save(any());
    }

    @Test
    @DisplayName("승인하면 APPROVED 로 저장하고 커밋 후 번역 이벤트를 발행한다")
    void approvePublishesEvent() {
        draftInStore();

        NoticeResult result = sut.approve(new NoticeActionCommand(OFFICE, NOTICE, "11"));

        assertThat(result.status()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(result.approvedBy()).isEqualTo("11");
        assertThat(result.activeToday()).isTrue();
        verify(eventPublisher).publishEvent(new NoticeApprovedEvent(NOTICE));
        verify(translateNoticeUseCase, never()).translateMissing(any());
    }

    @Test
    @DisplayName("다른 Office 의 공지는 NOT_FOUND 로 숨긴다")
    void otherOfficeNoticeHidden() {
        Notice other = Notice.register(99L, NoticeCategory.OTHER, "t", "b", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15), 11L, NOW);
        other.assignId(NOTICE);
        given(loadNoticePort.findById(NOTICE)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> sut.approve(new NoticeActionCommand(OFFICE, NOTICE, "11")))
                .isInstanceOf(NoticeException.class)
                .extracting(e -> ((NoticeException) e).getErrorType())
                .isEqualTo(NoticeException.ErrorType.NOT_FOUND);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("본문을 바꾸는 수정은 DRAFT·version 2 로 돌아가고, 날짜만 바꾸면 승인 상태가 남는다")
    void updateSemantics() {
        Notice n = draftInStore();
        n.approve("11", NOW, LocalDate.of(2027, 2, 1));

        NoticeResult datesOnly = sut.update(new UpdateNoticeCommand(OFFICE, NOTICE, NoticeCategory.HOUSING, "기숙사 신청",
                "2월 10일까지", null, List.of(), "2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 4, 1)));
        assertThat(datesOnly.status()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(datesOnly.version()).isEqualTo(1);
        assertThat(datesOnly.expiresAt()).isEqualTo(LocalDate.of(2027, 4, 1));

        NoticeResult edited = sut.update(new UpdateNoticeCommand(OFFICE, NOTICE, NoticeCategory.HOUSING, "기숙사 신청",
                "2월 12일까지", null, List.of(), "2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 4, 1)));
        assertThat(edited.status()).isEqualTo(NoticeStatus.DRAFT);
        assertThat(edited.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("재번역은 번역 유스케이스를 동기로 부르고 최신 상태를 돌려준다")
    void retranslateDelegates() {
        draftInStore();
        given(translateNoticeUseCase.translateMissing(NOTICE))
                .willReturn(new TranslateNoticeUseCase.Outcome(List.of(NoticeLang.EN), List.of()));

        NoticeResult result = sut.retranslate(new NoticeActionCommand(OFFICE, NOTICE, "11"));

        assertThat(result.id()).isEqualTo(NOTICE);
        verify(translateNoticeUseCase).translateMissing(NOTICE);
    }
}
