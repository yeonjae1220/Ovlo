package me.yeonjae.ovlo.application.service.command;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class NoticeReviewScheduleServiceTest {
    @Test void failedDeliveryIsRetriedWithoutExtendingReviewAndSuccessfulDeliveryIsDeduplicated() {
        Instant now=Instant.parse("2026-11-13T15:00:00Z");LocalDate today=LocalDate.of(2026,11,14);
        var knowledge=mock(NoticeKnowledgePort.class);var notices=mock(LoadNoticePort.class);var offices=mock(LoadOfficePort.class);
        var members=mock(LoadMemberPort.class);var notifications=mock(OfficeNotificationPort.class);
        Notice n=Notice.register(1L,NoticeCategory.OTHER,"T","B",null,List.of(),"2027-1",today.minusDays(60),today.plusDays(60),2L,now);
        n.assignId(3L);n.approve("2",now,today.minusDays(31));
        var source=new NoticeSource(NoticeSource.Type.OFFICE_MATERIAL,"국제처","문서","학생","전체",today.minusDays(31),null,2L,false,false,null,List.of());
        n.restoreGovernance(0,source,new NoticeReview(n.getVersion(),today.minusDays(31),today.minusDays(1),null));
        Office office=Office.create(1L,"국제처",now);office.invite(2L,OfficeRole.OFFICE_ADMIN,"test",now);office.accept(2L,now);
        Member member=Member.create("staff","담당자","Seoul",new Email("staff@example.test"),new Password("hash"),new UniversityId(1L),new Major("IA",DegreeType.BACHELOR,4));
        when(knowledge.reviewDueIds(today.plusDays(2))).thenReturn(List.of(3L));when(notices.findById(3L)).thenReturn(Optional.of(n));
        when(offices.findById(1L)).thenReturn(Optional.of(office));when(members.findAllByIds(anyList())).thenReturn(List.of(member));
        AtomicBoolean sent=new AtomicBoolean();when(knowledge.reminderSent(3L,today.minusDays(1),true)).thenAnswer(i->sent.get());
        doAnswer(i->{sent.set(true);return null;}).when(knowledge).markReminder(3L,today.minusDays(1),true);
        doThrow(new IllegalStateException("SMTP failure")).doNothing().when(notifications).sendNoticeReviewDue(anyList(),anyString(),anyString(),any(),eq(true));
        var service=new NoticeReviewScheduleService(knowledge,notices,offices,members,notifications,new TransactionTemplate(mock(PlatformTransactionManager.class)),Clock.fixed(now,ZoneOffset.UTC));
        assertThat(service.remind()).isZero();assertThat(service.remind()).isEqualTo(1);assertThat(service.remind()).isZero();
        assertThat(n.getReview().nextReviewOn()).isEqualTo(today.minusDays(1));
        verify(notifications,times(2)).sendNoticeReviewDue(anyList(),anyString(),anyString(),eq(today.minusDays(1)),eq(true));
    }
}
