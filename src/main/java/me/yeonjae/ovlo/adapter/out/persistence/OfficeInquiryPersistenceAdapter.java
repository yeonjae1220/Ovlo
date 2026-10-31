package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeInquiryFeedbackJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeInquiryJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.OfficeInquiryFeedbackJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.OfficeInquiryJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.application.port.out.office.SaveInquiryPort;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OfficeInquiryPersistenceAdapter implements LoadInquiryPort, SaveInquiryPort {

    /** 기간 상한이 없을 때의 경계값(timestamptz 범위 안). */
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T00:00:00Z");

    private final OfficeInquiryJpaRepository repository;
    private final OfficeInquiryFeedbackJpaRepository feedbackRepository;

    public OfficeInquiryPersistenceAdapter(OfficeInquiryJpaRepository repository,
                                           OfficeInquiryFeedbackJpaRepository feedbackRepository) {
        this.repository = repository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    public Optional<Inquiry> findById(Long inquiryId) {
        return repository.findById(inquiryId).map(this::toDomain);
    }

    @Override
    public Page<Inquiry> findByOffice(Long officeId, InquiryOutcome outcome, Instant from, Instant to, Pageable pageable) {
        Instant fromBound = from != null ? from : Instant.EPOCH;
        Instant toBound = to != null ? to : FAR_FUTURE;
        return repository.findByOfficeFiltered(officeId, outcome != null ? outcome.name() : null, fromBound, toBound, pageable)
                .map(this::toDomain);
    }

    @Override
    public long countByOfficeSince(Long officeId, Instant since) {
        return repository.countByOfficeIdAndAskedAtGreaterThanEqual(officeId, since);
    }

    @Override
    public Optional<InquiryFeedback> findFeedback(Long inquiryId) {
        return feedbackRepository.findById(inquiryId)
                .map(f -> new InquiryFeedback(f.getInquiryId(), f.isHelpful(), f.getReason(), f.getCreatedAt()));
    }

    @Override
    @Transactional
    public Inquiry save(Inquiry inquiry) {
        OfficeInquiryJpaEntity e = inquiry.getId() == null
                ? new OfficeInquiryJpaEntity()
                : repository.findById(inquiry.getId())
                        .orElseThrow(() -> new IllegalStateException("저장할 문의가 없습니다: id=" + inquiry.getId()));
        e.setOfficeId(inquiry.getOfficeId());
        e.setMemberId(inquiry.getMemberId());
        e.setLang(inquiry.getLang().code());
        e.setQuestion(inquiry.getQuestion());
        e.setOutcome(inquiry.getOutcome().name());
        e.setAnswerText(inquiry.getAnswerText());
        e.setCitedNoticeIds(join(inquiry.getCitedNoticeIds()));
        e.setCandidateNoticeIds(join(inquiry.getCandidateNoticeIds()));
        e.setAskedAt(inquiry.getAskedAt());
        e.setLatencyMs(inquiry.getLatencyMs());
        e.setModelId(inquiry.getModelId());
        OfficeInquiryJpaEntity saved = repository.saveAndFlush(e);
        if (inquiry.getId() == null) inquiry.assignId(saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void saveFeedback(InquiryFeedback feedback) {
        OfficeInquiryFeedbackJpaEntity e = feedbackRepository.findById(feedback.inquiryId())
                .orElseGet(OfficeInquiryFeedbackJpaEntity::new);
        e.setInquiryId(feedback.inquiryId());
        e.setHelpful(feedback.helpful());
        e.setReason(feedback.reason());
        e.setCreatedAt(feedback.createdAt());
        feedbackRepository.save(e);
    }

    private Inquiry toDomain(OfficeInquiryJpaEntity e) {
        return Inquiry.restore(e.getId(), e.getOfficeId(), e.getMemberId(),
                NoticeLang.fromCode(e.getLang()).orElse(NoticeLang.KO), e.getQuestion(),
                InquiryOutcome.valueOf(e.getOutcome()), e.getAnswerText(), split(e.getCitedNoticeIds()),
                split(e.getCandidateNoticeIds()), e.getAskedAt(), e.getLatencyMs(), e.getModelId());
    }

    private static String join(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> split(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
    }
}
