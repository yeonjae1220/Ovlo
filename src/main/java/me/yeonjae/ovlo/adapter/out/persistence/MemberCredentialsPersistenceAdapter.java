package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.repository.MemberJpaRepository;
import me.yeonjae.ovlo.application.dto.result.MemberCredentials;
import me.yeonjae.ovlo.application.port.out.auth.LoadMemberCredentialsPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MemberCredentialsPersistenceAdapter implements LoadMemberCredentialsPort {

    private final MemberJpaRepository memberJpaRepository;

    public MemberCredentialsPersistenceAdapter(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public Optional<MemberCredentials> findByEmail(String email) {
        return memberJpaRepository.findByEmail(email)
                .map(e -> new MemberCredentials(new MemberId(e.getId()), e.getPassword()));
    }
}
