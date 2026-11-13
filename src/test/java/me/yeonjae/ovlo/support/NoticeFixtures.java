package me.yeonjae.ovlo.support;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.office.*;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
public final class NoticeFixtures {
    public static void verify(Notice n,Instant now) {
        LocalDate day=LocalDate.ofInstant(now,OfficeDates.ZONE);
        if(day.isAfter(n.getExpiresAt())) day=n.getExpiresAt();
        NoticeSource source=new NoticeSource(NoticeSource.Type.OFFICE_MATERIAL,"국제처","fixture 안내문","학생","전체",day,null,n.getCreatedBy(),false,true,null,List.of());
        n.restoreGovernance(n.getRowVersion(),source,NoticeReview.pending()); n.verifySource(day);
    }
    public static NoticeGovernanceService governance(LoadNoticePort load) {
        return new NoticeGovernanceService(load,mock(SaveNoticePort.class),mock(LoadOfficePort.class),mock(NoticeKnowledgePort.class),Clock.systemUTC());
    }
}
