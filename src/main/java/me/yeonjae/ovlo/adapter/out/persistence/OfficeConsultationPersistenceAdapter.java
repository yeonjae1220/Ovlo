package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.ChatMessageTranslationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeConsultationJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.ChatMessageTranslationJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.OfficeConsultationJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.MessageTranslationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class OfficeConsultationPersistenceAdapter implements LoadConsultationPort, SaveConsultationPort, MessageTranslationPort {

    private final OfficeConsultationJpaRepository repository;
    private final ChatMessageTranslationJpaRepository translationRepository;

    public OfficeConsultationPersistenceAdapter(OfficeConsultationJpaRepository repository,
                                                ChatMessageTranslationJpaRepository translationRepository) {
        this.repository = repository;
        this.translationRepository = translationRepository;
    }

    @Override
    public Optional<Consultation> findById(Long consultationId) {
        return repository.findById(consultationId).map(this::toDomain);
    }

    @Override
    public Optional<Consultation> findByChatRoomId(Long chatRoomId) {
        return repository.findByChatRoomId(chatRoomId).map(this::toDomain);
    }

    @Override
    public Page<Consultation> findByOffice(Long officeId, ConsultationStatus status, Pageable pageable) {
        return repository.findByOfficeFiltered(officeId, status != null ? status.name() : null, pageable).map(this::toDomain);
    }

    @Override
    public List<Consultation> findByMember(Long officeId, Long memberId) {
        return repository.findByOfficeIdAndMemberIdOrderByOpenedAtDesc(officeId, memberId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Consultation> findActiveByOffice(Long officeId) {
        return repository.findByOfficeIdAndStatusNot(officeId, ConsultationStatus.CLOSED.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Long> findIdsResolvedBefore(Instant cutoff) {
        return repository.findIdsResolvedBefore(cutoff);
    }

    @Override
    @Transactional
    public Consultation save(Consultation c) {
        OfficeConsultationJpaEntity e = c.getId() == null
                ? new OfficeConsultationJpaEntity()
                : repository.findById(c.getId())
                        .orElseThrow(() -> new IllegalStateException("저장할 상담이 없습니다: id=" + c.getId()));
        e.setOfficeId(c.getOfficeId());
        e.setMemberId(c.getMemberId());
        e.setInquiryId(c.getInquiryId());
        e.setConsentVersion(c.getConsentVersion());
        e.setConsentText(c.getConsentText());
        e.setSharedQuestion(c.getSharedContext().question());
        e.setSharedAnswer(c.getSharedContext().answerText());
        e.setSharedLang(c.getSharedContext().lang().code());
        e.setSharedVerifiedStudent(c.getSharedContext().verifiedStudent());
        e.setStatus(c.getStatus().name());
        e.setAssignedStaffMemberId(c.getAssignedStaffMemberId());
        e.setChatRoomId(c.getChatRoomId());
        e.setOpenedAt(c.getOpenedAt());
        e.setFirstStaffReplyAt(c.getFirstStaffReplyAt());
        e.setResolvedAt(c.getResolvedAt());
        e.setClosedAt(c.getClosedAt());
        OfficeConsultationJpaEntity saved = repository.saveAndFlush(e);
        if (c.getId() == null) c.assignId(saved.getId());
        return toDomain(saved);
    }

    // ── MessageTranslationPort ───────────────────────────────────────────────

    @Override
    public Map<Long, String> findTranslations(List<Long> messageIds, NoticeLang lang) {
        if (messageIds.isEmpty()) return Map.of();
        return translationRepository.findByMessageIdInAndLang(messageIds, lang.code()).stream()
                .collect(Collectors.toMap(ChatMessageTranslationJpaEntity::getMessageId, ChatMessageTranslationJpaEntity::getText));
    }

    @Override
    @Transactional
    public void save(Long messageId, NoticeLang lang, String text, String modelId) {
        ChatMessageTranslationJpaEntity e = translationRepository.findByMessageIdAndLang(messageId, lang.code())
                .orElseGet(ChatMessageTranslationJpaEntity::new);
        e.setMessageId(messageId);
        e.setLang(lang.code());
        e.setText(text);
        e.setModelId(modelId);
        translationRepository.save(e);
    }

    private Consultation toDomain(OfficeConsultationJpaEntity e) {
        return Consultation.restore(e.getId(), e.getOfficeId(), e.getMemberId(), e.getInquiryId(), e.getConsentVersion(),
                e.getConsentText(),
                new Consultation.SharedContext(e.getSharedQuestion(), e.getSharedAnswer(),
                        NoticeLang.fromCode(e.getSharedLang()).orElse(NoticeLang.KO), e.isSharedVerifiedStudent()),
                ConsultationStatus.valueOf(e.getStatus()), e.getAssignedStaffMemberId(), e.getChatRoomId(), e.getOpenedAt(),
                e.getFirstStaffReplyAt(), e.getResolvedAt(), e.getClosedAt());
    }
}
