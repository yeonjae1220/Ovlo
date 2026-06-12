package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.repository.MemberJpaRepository;
import me.yeonjae.ovlo.application.port.out.verification.LoadMemberHomeUniversityPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * verification BC의 {@link LoadMemberHomeUniversityPort}를 member 영속에 연결하는 어댑터.
 * 본교 대학 ID만 프로젝션해 조회한다(엔티티 전체 로딩 회피).
 */
@Component
public class MemberHomeUniversityAdapter implements LoadMemberHomeUniversityPort {

    private final MemberJpaRepository memberJpaRepository;

    public MemberHomeUniversityAdapter(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public Optional<Long> findHomeUniversityId(Long memberId) {
        return memberJpaRepository.findHomeUniversityIdById(memberId);
    }
}
