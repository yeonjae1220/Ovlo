package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 없거나 stale 인 번역을 언어별로 생성한다. 언어마다 독립적으로 시도해 한 언어의 실패가 다른 언어를 막지 않는다.
 * 게이트에 걸리거나 포트가 예외를 던진 언어는 저장하지 않고(빈 값 치환 금지) 담당자에게 알린다.
 */
@Service
public class NoticeTranslationService implements TranslateNoticeUseCase {

    private final me.yeonjae.ovlo.application.port.out.office.NoticeTranslationVerificationPort verifier;
    private final me.yeonjae.ovlo.application.port.out.office.NoticeTranslationJobPort jobs;

    private static final Logger log = LoggerFactory.getLogger(NoticeTranslationService.class);

    private final me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort admission;
    private final LoadNoticePort loadNoticePort;
    private final SaveNoticePort saveNoticePort;
    private final LoadOfficePort loadOfficePort;
    private final LoadMemberPort loadMemberPort;
    private final TranslationPort translationPort;
    private final NoticeTranslationGate gate;
    private final OfficeNotificationPort notificationPort;
    private final Clock clock;
    private final me.yeonjae.ovlo.application.port.out.office.TranslationWorkPort work;
    private final org.springframework.transaction.support.TransactionTemplate tx;
    private final me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort knowledge;

    public NoticeTranslationService(LoadNoticePort loadNoticePort, SaveNoticePort saveNoticePort,
                                    LoadOfficePort loadOfficePort, LoadMemberPort loadMemberPort,
                                    TranslationPort translationPort, NoticeTranslationGate gate,
                                    OfficeNotificationPort notificationPort, Clock clock, org.springframework.transaction.support.TransactionTemplate tx,
                                    me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort knowledge,
                                    me.yeonjae.ovlo.application.port.out.office.TranslationWorkPort work,
                                    me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort admission,
                                    me.yeonjae.ovlo.application.port.out.office.NoticeTranslationVerificationPort verifier,
                                    me.yeonjae.ovlo.application.port.out.office.NoticeTranslationJobPort jobs) {
        this.verifier=verifier; this.jobs=jobs;
        this.loadNoticePort = loadNoticePort;
        this.saveNoticePort = saveNoticePort;
        this.loadOfficePort = loadOfficePort;
        this.loadMemberPort = loadMemberPort;
        this.translationPort = translationPort;
        this.gate = gate;
        this.notificationPort = notificationPort;
        this.clock = clock; this.tx=tx; this.knowledge=knowledge; this.work=work; this.admission=admission;
    }

    @Override
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Outcome translateMissing(Long noticeId) {
        Notice notice = loadNoticePort.findById(noticeId)
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다: id=" + noticeId,
                        NoticeException.ErrorType.NOT_FOUND));
        List<NoticeLang> generated = new ArrayList<>();
        List<NoticeLang> failed = new ArrayList<>();
        List<NoticeLang> deferred = new ArrayList<>();
        if (notice.getStatus()==me.yeonjae.ovlo.domain.office.model.NoticeStatus.ARCHIVED) return new Outcome(List.of(),List.of(),List.of());
        Office office=loadOfficePort.findById(notice.getOfficeId()).orElse(null);
        if (office==null || office.isSuspended()) return new Outcome(List.of(),List.of(),List.of());
        List<NoticeLang> targets = notice.translationsToGenerate(NoticeLang.ALL);
        jobs.enqueue(noticeId, notice.getVersion(), targets);
        for (NoticeLang lang : targets) {
            var claimedJob=jobs.claim(noticeId,notice.getVersion(),lang);
            if (claimedJob.isEmpty()) { deferred.add(lang); continue; }
            var job=claimedJob.get();
            TranslationPort.Request request = new TranslationPort.Request(notice.getSourceLang(), lang,notice.getTitle(),notice.getBody());
            try {
                var claimed = work.tryAcquire("notice:" + noticeId + ":" + notice.getVersion() + ":" + lang.code());
                if (claimed.isEmpty()) { jobs.defer(job,30); deferred.add(lang); continue; }
                try (var lease = claimed.get()) {
                    Notice latest = loadNoticePort.findCommittedById(noticeId).orElseThrow();
                    if (!current(latest,notice,lang)) { jobs.finish(job,false); continue; }
                    try (var admitted = admission.acquire(notice.getOfficeId(),notice.getCreatedBy(),2)) {
                        // Reserve an attempt durably before either paid call, including worker crashes.
                        if (!jobs.beginAttempt(job)) { jobs.finish(job,true); continue; }
                        TranslationPort.Result result=translationPort.translate(request);
                        var verdict=gate.check(request,result);
                        if (!verdict.passed() || !verifier.verifyTranslation(request,result)) {
                            jobs.finish(job,true);
                            failed.add(lang);
                            log.warn("[Notice] translation integrity rejected notice={} lang={} stage={}",noticeId,lang.code(),verdict.passed()?"semantic":"structure");
                            continue;
                        }
                        boolean stored=Boolean.TRUE.equals(tx.execute(status -> {
                            knowledge.lockOffice(notice.getOfficeId());
                            Notice current=loadNoticePort.findById(noticeId).orElseThrow();
                            Office currentOffice=loadOfficePort.findById(notice.getOfficeId()).orElseThrow();
                            if (!lease.isHeld() || !jobs.isHeld(job) || currentOffice.isSuspended() || !current(current,notice,lang)) return false;
                            current.putTranslation(lang,result.title(),result.body(),clock.instant());
                            saveNoticePort.save(current);
                            jobs.finish(job,false);
                            return true;
                        }));
                        if (stored) generated.add(lang);
                        else jobs.finish(job,false);
                    }
                }
            } catch (me.yeonjae.ovlo.application.port.out.office.TranslationAdmissionPort.Unavailable e) {
                jobs.defer(job,e.retryAfterSeconds());
                deferred.add(lang);
            } catch (RuntimeException e) {
                log.warn("[Notice] translation failed notice={} lang={}: {}",noticeId,lang.code(),e.getClass().getSimpleName());
                jobs.defer(job,300);
                failed.add(lang);
            }
        }
        if (!failed.isEmpty()) notifyAdmins(notice,failed);
        return new Outcome(List.copyOf(generated),List.copyOf(failed),List.copyOf(deferred));
    }

    private boolean current(Notice latest, Notice original, NoticeLang lang) {
        return latest.getVersion()==original.getVersion()
                && latest.getStatus()!=me.yeonjae.ovlo.domain.office.model.NoticeStatus.ARCHIVED
                && !latest.getExpiresAt().isBefore(me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock))
                && latest.getTranslation(lang).filter(t -> !t.isStaleFor(latest.getVersion())).isEmpty();
    }

    @Override @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public Outcome retryManually(Long id) {
        var notice=loadNoticePort.findCommittedById(id).orElseThrow();
        jobs.reopenRejected(id,notice.getVersion());
        return translateMissing(id);
    }

    @Override @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void retryDue() {
        for (Long id:jobs.dueNotices()) {
            try { translateMissing(id); }
            catch (RuntimeException e) { log.warn("Notice translation retry failed notice={} type={}",id,e.getClass().getSimpleName()); }
        }
    }

    private void notifyAdmins(Notice notice, List<NoticeLang> failed) {
        Office office = loadOfficePort.findById(notice.getOfficeId()).orElse(null);
        if (office == null) return;
        List<MemberId> staffIds = office.activeStaff().stream().map(OfficeStaff::getMemberId).map(MemberId::new).toList();
        List<String> emails = loadMemberPort.findAllByIds(staffIds).stream().map(m -> m.getEmail().value()).toList();
        notificationPort.sendNoticeTranslationFailed(emails, office.getName(), notice.getTitle(),
                failed.stream().map(NoticeLang::code).toList());
    }
}
