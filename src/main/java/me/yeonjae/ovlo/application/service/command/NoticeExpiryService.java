package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.in.office.ExpireNoticesUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * 공지 만료·만료 임박 알림. 대상 id 목록을 먼저 읽고 **한 건씩 별도 트랜잭션**으로 처리한다 — 클래스 레벨
 * {@code @Transactional} 로 묶고 예외를 삼키면 한 건의 실패가 그날 전원을 되돌린다(GLOBAL-PIT-128).
 * 실패한 건은 기록만 하고 다음 날 다시 대상이 된다.
 */
@Service
public class NoticeExpiryService implements ExpireNoticesUseCase {

    private static final Logger log = LoggerFactory.getLogger(NoticeExpiryService.class);

    private final LoadNoticePort loadNoticePort;
    private final SaveNoticePort saveNoticePort;
    private final LoadOfficePort loadOfficePort;
    private final LoadMemberPort loadMemberPort;
    private final OfficeNotificationPort notificationPort;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int warnDaysAhead;

    public NoticeExpiryService(LoadNoticePort loadNoticePort, SaveNoticePort saveNoticePort, LoadOfficePort loadOfficePort,
                               LoadMemberPort loadMemberPort, OfficeNotificationPort notificationPort,
                               TransactionTemplate tx, Clock clock,
                               @Value("${ovlo.office.notice-expiry-warn-days:7}") int warnDaysAhead) {
        this.loadNoticePort = loadNoticePort;
        this.saveNoticePort = saveNoticePort;
        this.loadOfficePort = loadOfficePort;
        this.loadMemberPort = loadMemberPort;
        this.notificationPort = notificationPort;
        this.tx = tx;
        this.clock = clock;
        this.warnDaysAhead = warnDaysAhead;
    }

    @Override
    public int expireDue() {
        LocalDate today = today();
        int changed = 0;
        for (Long id : loadNoticePort.findIdsToExpire(today)) {
            try {
                Boolean expired = tx.execute(status -> loadNoticePort.findById(id).map(n -> {
                    boolean did = n.expire(today);
                    if (did) saveNoticePort.save(n);
                    return did;
                }).orElse(false));
                if (Boolean.TRUE.equals(expired)) changed++;
            } catch (RuntimeException e) {
                log.error("[Notice] expire failed notice={} (다음 실행에서 재시도)", id, e);
            }
        }
        if (changed > 0) log.info("[Notice] expired {} notice(s) as of {}", changed, today);
        return changed;
    }

    @Override
    public int warnExpiringSoon() {
        LocalDate today = today();
        int warned = 0;
        for (Long id : loadNoticePort.findIdsExpiringWithin(today, warnDaysAhead)) {
            try {
                Boolean did = tx.execute(status -> loadNoticePort.findById(id).map(n -> {
                    if (!n.needsExpiryWarning(today, warnDaysAhead)) return false;
                    sendWarning(n);
                    n.markExpiryWarned(clock.instant());
                    saveNoticePort.save(n);
                    return true;
                }).orElse(false));
                if (Boolean.TRUE.equals(did)) warned++;
            } catch (RuntimeException e) {
                log.error("[Notice] expiry warning failed notice={} (다음 실행에서 재시도)", id, e);
            }
        }
        return warned;
    }

    private void sendWarning(Notice notice) {
        Office office = loadOfficePort.findById(notice.getOfficeId()).orElse(null);
        if (office == null) return;
        List<MemberId> staffIds = office.activeStaff().stream().map(OfficeStaff::getMemberId).map(MemberId::new).toList();
        List<String> emails = loadMemberPort.findAllByIds(staffIds).stream().map(m -> m.getEmail().value()).toList();
        notificationPort.sendNoticeExpiringSoon(emails, office.getName(), notice.getTitle(), notice.getExpiresAt());
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), clock.getZone());
    }
}
