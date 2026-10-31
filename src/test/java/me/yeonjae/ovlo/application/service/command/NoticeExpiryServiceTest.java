package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
class NoticeExpiryServiceTest {

    // today = 2027-03-16
    private static final Instant NOW = Instant.parse("2027-03-16T01:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 16);

    @Mock LoadNoticePort loadNoticePort;
    @Mock SaveNoticePort saveNoticePort;
    @Mock LoadOfficePort loadOfficePort;
    @Mock LoadMemberPort loadMemberPort;
    @Mock OfficeNotificationPort notificationPort;
    @Mock TransactionTemplate tx;

    private NoticeExpiryService sut;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sut = new NoticeExpiryService(loadNoticePort, saveNoticePort, loadOfficePort, loadMemberPort, notificationPort,
                tx, Clock.fixed(NOW, ZoneOffset.UTC), 7);
        lenient().when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        lenient().when(saveNoticePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Office office = Office.create(266L, "국제처", NOW);
        office.assignId(new OfficeId(5L));
        lenient().when(loadOfficePort.findById(5L)).thenReturn(Optional.of(office));
        lenient().when(loadMemberPort.findAllByIds(anyList())).thenReturn(List.of());
    }

    private static Notice approvedExpiring(Long id, LocalDate expiresAt) {
        Notice n = Notice.register(5L, NoticeCategory.HOUSING, "공지 " + id, "본문", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), expiresAt, 11L, Instant.parse("2027-02-01T00:00:00Z"));
        n.assignId(id);
        n.approve("11", Instant.parse("2027-02-01T00:00:00Z"), LocalDate.of(2027, 2, 1));
        return n;
    }

    @Test
    @DisplayName("만료 대상은 한 건씩 트랜잭션으로 처리하고, 한 건의 실패가 다른 건을 막지 않는다 (GLOBAL-PIT-128)")
    void expiresPerItemAndIsolatesFailure() {
        Notice a = approvedExpiring(1L, LocalDate.of(2027, 3, 15));
        Notice c = approvedExpiring(3L, LocalDate.of(2027, 3, 10));
        given(loadNoticePort.findIdsToExpire(TODAY)).willReturn(List.of(1L, 2L, 3L));
        given(loadNoticePort.findById(1L)).willReturn(Optional.of(a));
        given(loadNoticePort.findById(2L)).willThrow(new IllegalStateException("db hiccup"));
        given(loadNoticePort.findById(3L)).willReturn(Optional.of(c));

        int changed = sut.expireDue();

        assertThat(changed).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(NoticeStatus.EXPIRED);
        assertThat(c.getStatus()).isEqualTo(NoticeStatus.EXPIRED);
        verify(tx, org.mockito.Mockito.times(3)).execute(any());
    }

    @Test
    @DisplayName("만료 7일 전 공지는 담당자에게 한 번만 알리고 표시한다")
    void warnsOnceWithinWindow() {
        Notice soon = approvedExpiring(10L, LocalDate.of(2027, 3, 20));
        given(loadNoticePort.findIdsExpiringWithin(TODAY, 7)).willReturn(List.of(10L));
        given(loadNoticePort.findById(10L)).willReturn(Optional.of(soon));

        assertThat(sut.warnExpiringSoon()).isEqualTo(1);
        assertThat(soon.getExpiryWarnedAt()).isEqualTo(NOW);
        verify(notificationPort).sendNoticeExpiringSoon(anyList(), eq("국제처"), eq("공지 10"), eq(LocalDate.of(2027, 3, 20)));

        // 두 번째 실행: 포트가 다시 돌려줘도 도메인이 '이미 알림'으로 걸러낸다
        assertThat(sut.warnExpiringSoon()).isZero();
        verify(notificationPort, org.mockito.Mockito.times(1)).sendNoticeExpiringSoon(any(), any(), any(), any());
    }

    @Test
    @DisplayName("대상이 없으면 아무것도 저장하지 않는다")
    void nothingToDo() {
        given(loadNoticePort.findIdsToExpire(TODAY)).willReturn(List.of());

        assertThat(sut.expireDue()).isZero();
        verify(saveNoticePort, never()).save(any());
    }
}
