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
 * 승인 자료 등록·수정·승인·보관·번역 검토. 현재 담당자 소속과 국제처 상태를 확인하고,
 * 자료 소유 학교·요청 revision·출처 검토 자격을 변경 시점에 검증한다.
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
    private final me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort knowledge;
    private final me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance;

    public NoticeCommandService(LoadNoticePort loadNoticePort, SaveNoticePort saveNoticePort, LoadOfficePort loadOfficePort,
                                TranslateNoticeUseCase translateNoticeUseCase, ApplicationEventPublisher eventPublisher,
                                Clock clock, me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort knowledge,
                                me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance) {
        this.loadNoticePort = loadNoticePort;
        this.saveNoticePort = saveNoticePort;
        this.loadOfficePort = loadOfficePort;
        this.translateNoticeUseCase = translateNoticeUseCase;
        this.eventPublisher = eventPublisher;
        this.clock = clock; this.knowledge=knowledge; this.governance=governance;
    }

    @Override
    public NoticeResult register(RegisterNoticeCommand c) {
        knowledge.lockOffice(c.officeId());
        requireWritableOffice(c.officeId());
        governance.authorize(c.officeId(),c.createdBy(),false);
        Notice notice = Notice.register(c.officeId(), c.category(), c.title(), c.body(), c.sourceUrl(),
                c.attachmentMediaIds(), c.term(), c.effectiveFrom(), c.expiresAt(), c.createdBy(), clock.instant());
        Notice saved = saveNoticePort.save(notice);
        log.info("[Notice] registered office={} notice={} by={}", c.officeId(), saved.getId(), c.createdBy());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public NoticeResult update(UpdateNoticeCommand c) {
        knowledge.lockOffice(c.officeId());
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        if (c.revision()!=null) notice.expectRevision(c.revision());
        if(c.actor()!=null) governance.authorize(c.officeId(),c.actor(),false);
        int previousVersion=notice.getVersion();
        notice.revise(c.category(),c.title(),c.body(),c.sourceUrl(),c.attachmentMediaIds(),c.term(),c.effectiveFrom(),c.expiresAt(),clock.instant());
        Notice saved = saveNoticePort.save(notice);
        if(saved.getVersion()!=previousVersion) governance.invalidateDependents(c.officeId(),saved.getId());
        knowledge.audit(saved,c.actor()==null?0:c.actor(),"UPDATED","자료 수정",clock.instant());
        log.info("[Notice] updated office={} notice={} version={} status={}", c.officeId(), saved.getId(),
                saved.getVersion(), saved.getStatus());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public NoticeResult approve(NoticeActionCommand c) {
        knowledge.lockOffice(c.officeId());
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        if (c.revision()!=null) notice.expectRevision(c.revision());
        governance.authorize(c.officeId(),Long.valueOf(c.actor()),false);
        governance.validateApproval(notice);
        notice.approve(c.actor(), clock.instant(), today());
        Notice saved = saveNoticePort.save(notice);
        knowledge.audit(saved,Long.parseLong(c.actor()),"APPROVED","검토한 현재 버전 승인",clock.instant());
        // 커밋 뒤 비동기 번역(NoticeTranslationEventListener)
        eventPublisher.publishEvent(new NoticeApprovedEvent(saved.getId()));
        log.info("[Notice] approved office={} notice={} by={} expiresAt={}", c.officeId(), saved.getId(), c.actor(),
                saved.getExpiresAt());
        return NoticeResultMapper.toResult(saved, today());
    }

    @Override
    public void archive(NoticeActionCommand c) {
        knowledge.lockOffice(c.officeId());
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        if (c.revision()!=null) notice.expectRevision(c.revision());
        governance.authorize(c.officeId(),Long.valueOf(c.actor()),false);
        notice.archive();
        saveNoticePort.save(notice);
        governance.invalidateDependents(c.officeId(),notice.getId());
        knowledge.audit(notice,Long.parseLong(c.actor()),"ARCHIVED","보관",clock.instant());
        log.info("[Notice] archived office={} notice={} by={}", c.officeId(), c.noticeId(), c.actor());
    }

    @Override
    public void reviewTranslation(ReviewNoticeTranslationCommand c) {
        knowledge.lockOffice(c.officeId());
        requireWritableOffice(c.officeId());
        Notice notice = requireNotice(c.officeId(), c.noticeId());
        if(c.revision()!=null) notice.expectRevision(c.revision());
        if(c.actor()!=null) governance.authorize(c.officeId(),c.actor(),false);
        notice.reviewTranslation(c.lang());
        saveNoticePort.save(notice);
    }

    @Override
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public NoticeResult retranslate(NoticeActionCommand c) {
        requireWritableOffice(c.officeId());
        governance.authorize(c.officeId(),Long.valueOf(c.actor()),false);
        Notice notice=requireNotice(c.officeId(), c.noticeId());
        if(c.revision()!=null) notice.expectRevision(c.revision());
        translateNoticeUseCase.translateMissing(c.noticeId());
        Notice committed = loadNoticePort.findCommittedById(c.noticeId())
                .filter(n -> n.getOfficeId().equals(c.officeId()))
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다: id=" + c.noticeId(), NoticeException.ErrorType.NOT_FOUND));
        return NoticeResultMapper.toResult(committed, today());
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
        return me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock);
    }
}
