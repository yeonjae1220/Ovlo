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

    private static final Logger log = LoggerFactory.getLogger(NoticeTranslationService.class);

    private final LoadNoticePort loadNoticePort;
    private final SaveNoticePort saveNoticePort;
    private final LoadOfficePort loadOfficePort;
    private final LoadMemberPort loadMemberPort;
    private final TranslationPort translationPort;
    private final NoticeTranslationGate gate;
    private final OfficeNotificationPort notificationPort;
    private final Clock clock;

    public NoticeTranslationService(LoadNoticePort loadNoticePort, SaveNoticePort saveNoticePort,
                                    LoadOfficePort loadOfficePort, LoadMemberPort loadMemberPort,
                                    TranslationPort translationPort, NoticeTranslationGate gate,
                                    OfficeNotificationPort notificationPort, Clock clock) {
        this.loadNoticePort = loadNoticePort;
        this.saveNoticePort = saveNoticePort;
        this.loadOfficePort = loadOfficePort;
        this.loadMemberPort = loadMemberPort;
        this.translationPort = translationPort;
        this.gate = gate;
        this.notificationPort = notificationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Outcome translateMissing(Long noticeId) {
        Notice notice = loadNoticePort.findById(noticeId)
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다: id=" + noticeId,
                        NoticeException.ErrorType.NOT_FOUND));
        List<NoticeLang> targets = notice.translationsToGenerate(NoticeLang.ALL);
        List<NoticeLang> generated = new ArrayList<>();
        List<NoticeLang> failed = new ArrayList<>();

        for (NoticeLang lang : targets) {
            TranslationPort.Request request = new TranslationPort.Request(notice.getSourceLang(), lang,
                    notice.getTitle(), notice.getBody());
            try {
                TranslationPort.Result result = translationPort.translate(request);
                NoticeTranslationGate.Verdict verdict = gate.check(request, result);
                if (!verdict.passed()) {
                    log.warn("[Notice] translation rejected notice={} lang={} reasons={}", noticeId, lang.code(), verdict.reasons());
                    failed.add(lang);
                    continue;
                }
                notice.putTranslation(lang, result.title(), result.body(), clock.instant());
                generated.add(lang);
            } catch (RuntimeException e) {
                log.warn("[Notice] translation failed notice={} lang={}: {}", noticeId, lang.code(), e.toString());
                failed.add(lang);
            }
        }

        if (!generated.isEmpty()) {
            saveNoticePort.save(notice);
        }
        if (!failed.isEmpty()) {
            notifyAdmins(notice, failed);
        }
        return new Outcome(List.copyOf(generated), List.copyOf(failed));
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
