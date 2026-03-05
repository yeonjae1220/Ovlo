package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.result.MemberCredentials;
import me.yeonjae.ovlo.application.port.out.auth.LoadMemberCredentialsPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * member 테이블에서 이메일/해시 비밀번호를 조회하는 어댑터.
 * MemberJpaEntity와 MemberRepository는 member 도메인 JPA 구현 시 추가.
 * 현재는 컴파일용 스텁 — member 도메인 JPA 구현 후 교체.
 */
@Component
public class MemberCredentialsPersistenceAdapter implements LoadMemberCredentialsPort {

    // TODO: member 도메인 JPA 어댑터 구현 시 MemberJpaRepository 주입
    // private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<MemberCredentials> findByEmail(String email) {
        // TODO: 실제 구현
        // return memberJpaRepository.findByEmail(email)
        //     .map(e -> new MemberCredentials(new MemberId(e.getId()), e.getPassword()));
        throw new UnsupportedOperationException("member 도메인 JPA 구현 후 연결 필요");
    }
}
