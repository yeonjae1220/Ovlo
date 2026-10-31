package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.dto.result.StudentNoticeResult;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.service.office.NoticeResultMapper;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NoticeQueryService implements GetNoticeQuery {

    private final LoadNoticePort loadNoticePort;
    private final Clock clock;

    public NoticeQueryService(LoadNoticePort loadNoticePort, Clock clock) {
        this.loadNoticePort = loadNoticePort;
        this.clock = clock;
    }

    @Override
    public Page<NoticeSummaryResult> findForStaff(Long officeId, NoticeStatus status, NoticeCategory category, Pageable pageable) {
        return loadNoticePort.findByOffice(officeId, status, category, pageable).map(NoticeResultMapper::toSummary);
    }

    @Override
    public Optional<NoticeResult> findDetail(Long officeId, Long noticeId) {
        return loadNoticePort.findById(noticeId)
                .filter(n -> n.getOfficeId().equals(officeId))
                .map(n -> NoticeResultMapper.toResult(n, today()));
    }

    @Override
    public List<StudentNoticeResult> findForStudent(Long officeId, NoticeCategory category, NoticeLang lang) {
        return loadNoticePort.findActiveForStudent(officeId, category, today()).stream()
                .map(n -> NoticeResultMapper.toStudent(n, lang))
                .toList();
    }

    @Override
    public Optional<StudentNoticeResult> findStudentDetail(Long officeId, Long noticeId, NoticeLang lang) {
        LocalDate today = today();
        return loadNoticePort.findById(noticeId)
                .filter(n -> n.getOfficeId().equals(officeId))
                .filter(n -> n.isActiveOn(today))
                .map(n -> NoticeResultMapper.toStudent(n, lang));
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), clock.getZone());
    }
}
