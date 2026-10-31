package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.command.ReviewNoticeTranslationCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase;
import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.SaveNoticePort;
import me.yeonjae.ovlo.application.service.office.NoticeApprovedEvent;
import me.yeonjae.ovlo.application.service.office.NoticeResultMapper;
import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.Office;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 승인 자료 등록·수정·승인·보관·번역 검토. 범위 검사(어느 Office 의 담당자인가)는 어댑터의 OfficeAccessPolicy 가 하고,
 * 여기서는 "그 공지가 그 Office 의 것인가"와 Office 가 정지되지 않았는가만 본다.
 */
@Service
@Transactional
public class NoticeCommandService implements ManageNoticeUseCase {

    private static final Logger log = LoggerFactory.getLogger(NoticeCommandService.class);

    private final LoadNoticePort loadNoticePort;
    private final SaveNoticePort saveNoticePort;
    private final LoadOfficePort loadOfficePort;
    private final TranslateNoticeUseCase translateNoticeUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public NoticeCommandService(LoadNoticePort loadNoticePort, SaveNoticePort saveNoticePort, LoadOfficePort loadOfficePort,
                                TranslateNoticeUseCase translateNoticeUseCase, ApplicationEventPublisher eventPublisher,
                                Clock clock) {
        this.loadNoticePort = loadNoticePort;
        this.saveNoticePort = saveNoticePort;
        this.loadOfficePort = loadOfficePort;
        this.translateNoticeUseCase = translateNoticeUseCase;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public NoticeResult register(RegisterNoticeCommand c) {
        requireWritableOffice(c.officeId());
        Notice notice = Notice.register(c.officeId(), c.category(), c.title(), c.body(), c.sourceUrl(),
                c.attachmentMediaIds(), c.term(), c.effectiveFrom(), c.expiresAt(), c.createdBy(), clock.instant());
        Notice saved = saveNoticePort.save(notice);
        log.info("[Notice] registered office={} notice={} by={}", c.officeId(), saved.getId(), c.createdBy());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public NoticeResult update(UpdateNoticeCommand c) {
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        notice.editContent(c.category(), c.title(), c.body(), c.sourceUrl(), c.attachmentMediaIds(), clock.instant());
        notice.reschedule(c.effectiveFrom(), c.expiresAt(), clock.instant());
        notice.changeTerm(c.term(), clock.instant());
        Notice saved = saveNoticePort.save(notice);
        log.info("[Notice] updated office={} notice={} version={} status={}", c.officeId(), saved.getId(),
                saved.getVersion(), saved.getStatus());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public NoticeResult approve(NoticeActionCommand c) {
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        notice.approve(c.actor(), clock.instant(), today());
        Notice saved = saveNoticePort.save(notice);
        // 커밋 뒤 비동기 번역(NoticeTranslationEventListener)
        eventPublisher.publishEvent(new NoticeApprovedEvent(saved.getId()));
        log.info("[Notice] approved office={} notice={} by={} expiresAt={}", c.officeId(), saved.getId(), c.actor(),
                saved.getExpiresAt());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public void archive(NoticeActionCommand c) {
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        notice.archive();
        saveNoticePort.save(notice);
        log.info("[Notice] archived office={} notice={} by={}", c.officeId(), c.noticeId(), c.actor());
    }

    @Override
    public void reviewTranslation(ReviewNoticeTranslationCommand c) {
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        notice.reviewTranslation(c.lang());
        saveNoticePort.save(notice);
    }

    @Override
    public NoticeResult retranslate(NoticeActionCommand c) {
        requireWritableOffice(c.officeId());
        requireNotice(c.officeId(), c.noticeId());
        translateNoticeUseCase.translateMissing(c.noticeId());
        return NoticeResultMapper.toResult(requireNotice(c.officeId(), c.noticeId()), today());
    }

    private Office requireWritableOffice(Long officeId) {
        Office office = loadOfficePort.findById(officeId)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다: id=" + officeId,
                        OfficeException.ErrorType.NOT_FOUND));
        if (office.isSuspended()) {
            throw new OfficeException("정지된 국제처입니다", OfficeException.ErrorType.SUSPENDED);
        }
        return office;
    }

    private Notice requireNotice(Long officeId, Long noticeId) {
        return loadNoticePort.findById(noticeId)
                .filter(n -> n.getOfficeId().equals(officeId))
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다: id=" + noticeId,
                        NoticeException.ErrorType.NOT_FOUND));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), clock.getZone());
    }
}
