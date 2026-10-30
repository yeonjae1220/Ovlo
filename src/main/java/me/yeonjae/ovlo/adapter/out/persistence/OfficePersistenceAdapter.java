package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.entity.OfficeStaffJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.OfficeJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.SaveOfficePort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Office 애그리거트 영속. 담당자 행은 (office_id, member_id) 로 식별해 도메인 상태를 덮어쓴다.
 * 담당자 행은 지우지 않는다(REMOVED 상태만) — 재초대가 같은 행을 재사용한다.
 */
@Component
public class OfficePersistenceAdapter implements LoadOfficePort, SaveOfficePort, LoadOfficeMembershipPort {

    private final OfficeJpaRepository repository;

    public OfficePersistenceAdapter(OfficeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Office> findById(Long officeId) {
        return repository.findWithStaffById(officeId).map(this::toDomain);
    }

    @Override
    public Optional<Office> findByUniversityId(Long globalUnivId) {
        return repository.findWithStaffByGlobalUnivId(globalUnivId).map(this::toDomain);
    }

    @Override
    public Page<Office> findAll(Pageable pageable) {
        // 목록은 담당자 없이 로딩해도 되지만 toDomain 이 staff 를 읽으므로 LAZY 로딩이 트랜잭션 안에서 일어나야 한다.
        return repository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public Optional<Office> findOfficeWithInvitationFor(Long memberId) {
        return repository.findWithInvitationFor(memberId).stream().findFirst()
                // JOIN FETCH 가 INVITED 행만 가져오므로 전체 담당자를 다시 읽는다.
                .flatMap(e -> repository.findWithStaffById(e.getId()))
                .map(this::toDomain);
    }

    @Override
    public Optional<OfficeMembership> findActiveMembership(MemberId memberId) {
        return repository.findActiveMembershipRows(memberId.value()).stream().findFirst()
                .map(row -> new OfficeMembership((Long) row[0], OfficeRole.valueOf((String) row[1])));
    }

    @Override
    @Transactional
    public Office save(Office office) {
        OfficeJpaEntity entity = office.getId() == null
                ? new OfficeJpaEntity()
                : repository.findWithStaffById(office.getId().value())
                        .orElseThrow(() -> new IllegalStateException("저장할 Office 가 없습니다: id=" + office.getId().value()));
        entity.setGlobalUnivId(office.getGlobalUnivId());
        entity.setName(office.getName());
        entity.setStatus(office.getStatus().name());
        if (entity.getId() == null) {
            entity.setCreatedAt(office.getCreatedAt());
        }

        Map<Long, OfficeStaffJpaEntity> byMember = entity.getStaff().stream()
                .collect(Collectors.toMap(OfficeStaffJpaEntity::getMemberId, Function.identity()));
        for (OfficeStaff s : office.getStaff()) {
            OfficeStaffJpaEntity row = byMember.get(s.getMemberId());
            if (row == null) {
                row = new OfficeStaffJpaEntity();
                row.setMemberId(s.getMemberId());
                entity.addStaff(row);
            }
            row.setRole(s.getRole().name());
            row.setStatus(s.getStatus().name());
            row.setInvitedBy(s.getInvitedBy());
            row.setInvitedAt(s.getInvitedAt());
            row.setAcceptedAt(s.getAcceptedAt());
            row.setRemovedAt(s.getRemovedAt());
        }

        OfficeJpaEntity saved = repository.saveAndFlush(entity);
        return toDomain(saved);
    }

    private Office toDomain(OfficeJpaEntity e) {
        List<OfficeStaff> staff = e.getStaff().stream()
                .map(s -> OfficeStaff.restore(s.getId(), s.getMemberId(), OfficeRole.valueOf(s.getRole()),
                        OfficeStaffStatus.valueOf(s.getStatus()), s.getInvitedBy(), s.getInvitedAt(),
                        s.getAcceptedAt(), s.getRemovedAt()))
                .toList();
        return Office.restore(new OfficeId(e.getId()), e.getGlobalUnivId(), e.getName(),
                OfficeStatus.valueOf(e.getStatus()), e.getCreatedAt(), staff);
    }
}
