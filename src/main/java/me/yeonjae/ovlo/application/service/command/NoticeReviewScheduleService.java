package me.yeonjae.ovlo.application.service.command;
import me.yeonjae.ovlo.application.port.in.office.ReviewNoticeScheduleUseCase;
import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.service.office.OfficeDates;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.LoggerFactory;
import java.time.Clock;
import java.util.*;

@Service
public class NoticeReviewScheduleService implements ReviewNoticeScheduleUseCase {
    private final NoticeKnowledgePort knowledge; private final LoadNoticePort notices;
    private final LoadOfficePort offices; private final LoadMemberPort members;
    private final OfficeNotificationPort notifications; private final TransactionTemplate tx; private final Clock clock;
    public NoticeReviewScheduleService(NoticeKnowledgePort knowledge,LoadNoticePort notices,LoadOfficePort offices,LoadMemberPort members,
                                      OfficeNotificationPort notifications,TransactionTemplate tx,Clock clock) {
        this.knowledge=knowledge;this.notices=notices;this.offices=offices;this.members=members;this.notifications=notifications;this.tx=tx;this.clock=clock;
    }
    public int remind() {
        var today=OfficeDates.today(clock); int sent=0;
        for(Long id:knowledge.reviewDueIds(today.plusDays(2))) try {
            boolean did=Boolean.TRUE.equals(tx.execute(status -> {
                Notice initial=notices.findById(id).orElseThrow();knowledge.lockOffice(initial.getOfficeId());
                Notice n=notices.findById(id).orElseThrow(); var due=n.getReview().nextReviewOn();
                if(n.getStatus()!=NoticeStatus.APPROVED || n.getSource()==null || due==null || due.isAfter(today.plusDays(2))) return false;
                boolean overdue=today.isAfter(due);
                if(knowledge.reminderSent(id,due,overdue)) return false;
                Office office=offices.findById(n.getOfficeId()).orElseThrow();
                Set<Long> recipients=new HashSet<>();
                boolean ownerActive=office.activeMembershipOf(n.getSource().ownerMemberId()).isPresent();
                if(ownerActive) recipients.add(n.getSource().ownerMemberId());
                if(overdue || !ownerActive) office.activeStaff().stream().filter(OfficeStaff::isActiveAdmin).map(OfficeStaff::getMemberId).forEach(recipients::add);
                var emails=members.findAllByIds(recipients.stream().map(MemberId::new).toList()).stream().map(m -> m.getEmail().value()).toList();
                if(emails.isEmpty()) return false;
                notifications.sendNoticeReviewDue(emails,office.getName(),n.getTitle(),due,overdue);
                knowledge.markReminder(id,due,overdue); return true;
            }));
            if(did) sent++;
        } catch(RuntimeException e) { LoggerFactory.getLogger(getClass()).error("자료 검토 알림 실패 notice={} (다음 실행에서 재시도)",id,e); }
        return sent;
    }
}
