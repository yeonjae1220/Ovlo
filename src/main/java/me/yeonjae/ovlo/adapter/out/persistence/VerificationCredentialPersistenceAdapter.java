package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.VerificationCredentialJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.repository.VerificationCredentialJpaRepository;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationId;
import me.yeonjae.ovlo.domain.verification.model.VerificationStatus;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class VerificationCredentialPersistenceAdapter
        implements LoadVerificationCredentialPort, SaveVerificationCredentialPort {

    private final VerificationCredentialJpaRepository repository;

    public VerificationCredentialPersistenceAdapter(VerificationCredentialJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VerificationCredential> findByMemberId(Long memberId) {
        return repository.findByMemberId(memberId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveByVerifiedEmailAndMemberIdNot(String verifiedEmail, Long memberId) {
        return repository.existsActiveByVerifiedEmailAndMemberIdNot(verifiedEmail, memberId);
    }

    @Override
    @Transactional
    public VerificationCredential save(VerificationCredential c) {
        // UNIQUE(member_id, type): 동일 타입 기존 자격을 대체(upsert)
        repository.deleteByMemberIdAndType(c.getMemberId(), c.getType().name());
        VerificationCredentialJpaEntity entity = toEntity(c);
        return toDomain(repository.save(entity));
    }

    private VerificationCredentialJpaEntity toEntity(VerificationCredential c) {
        VerificationCredentialJpaEntity e = new VerificationCredentialJpaEntity();
        if (c.getId() != null) e.setId(c.getId().value());
        e.setMemberId(c.getMemberId());
        e.setType(c.getType().name());
        e.setUniversityId(c.getUniversityId());
        e.setVerifiedEmail(c.getVerifiedEmail());
        e.setStatus(c.getStatus().name());
        e.setVerifiedAt(c.getVerifiedAt());
        return e;
    }

    private VerificationCredential toDomain(VerificationCredentialJpaEntity e) {
        return VerificationCredential.restore(
                new VerificationId(e.getId()),
                e.getMemberId(),
                VerificationType.valueOf(e.getType()),
                e.getUniversityId(),
                e.getVerifiedEmail(),
                VerificationStatus.valueOf(e.getStatus()),
                e.getVerifiedAt());
    }
}
