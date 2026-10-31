package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.PartnerClickJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.PartnerJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PartnerClickJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PartnerJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadPartnerPort;
import me.yeonjae.ovlo.application.port.out.office.SavePartnerPort;
import me.yeonjae.ovlo.domain.office.model.Partner;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PartnerPersistenceAdapter implements LoadPartnerPort, SavePartnerPort {

    private final PartnerJpaRepository repository;
    private final PartnerClickJpaRepository clickRepository;

    public PartnerPersistenceAdapter(PartnerJpaRepository repository, PartnerClickJpaRepository clickRepository) {
        this.repository = repository;
        this.clickRepository = clickRepository;
    }

    @Override
    public Optional<Partner> findById(Long partnerId) {
        return repository.findById(partnerId).map(this::toDomain);
    }

    @Override
    public List<Partner> findAll() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Partner> findEnabled() {
        return repository.findByEnabledTrueOrderBySortOrderAscIdAsc().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public Partner save(Partner p) {
        PartnerJpaEntity e = p.getId() == null ? new PartnerJpaEntity()
                : repository.findById(p.getId()).orElseThrow(() -> new IllegalStateException("저장할 제휴가 없습니다: id=" + p.getId()));
        e.setCategory(p.getCategory().name());
        e.setName(p.getName());
        e.setDescription(p.getDescription());
        e.setTargetUrl(p.getTargetUrl());
        e.setEnabled(p.isEnabled());
        e.setSortOrder(p.getSortOrder());
        if (e.getId() == null) e.setCreatedAt(p.getCreatedAt());
        PartnerJpaEntity saved = repository.saveAndFlush(e);
        if (p.getId() == null) p.assignId(saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void recordClick(Long partnerId, Long officeId, Long memberId, Instant clickedAt) {
        PartnerClickJpaEntity c = new PartnerClickJpaEntity();
        c.setPartnerId(partnerId);
        c.setOfficeId(officeId);
        c.setMemberId(memberId);
        c.setClickedAt(clickedAt);
        clickRepository.save(c);
    }

    private Partner toDomain(PartnerJpaEntity e) {
        return Partner.restore(e.getId(), PartnerCategory.valueOf(e.getCategory()), e.getName(), e.getDescription(),
                e.getTargetUrl(), e.isEnabled(), e.getSortOrder(), e.getCreatedAt());
    }
}
