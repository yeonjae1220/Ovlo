package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.entity.MemberJpaEntity;
import me.yeonjae.ovlo.adapter.out.persistence.mapper.MemberMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MemberJpaRepository;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberMapper memberMapper;

    public MemberPersistenceAdapter(MemberJpaRepository memberJpaRepository, MemberMapper memberMapper) {
        this.memberJpaRepository = memberJpaRepository;
        this.memberMapper = memberMapper;
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        return memberJpaRepository.findById(memberId.value())
                .map(memberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return memberJpaRepository.findByEmail(email)
                .map(memberMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return memberJpaRepository.existsByNickname(nickname);
    }

    @Override
    public List<Member> searchByNickname(String keyword) {
        return memberJpaRepository.findByNicknameContainingIgnoreCase(keyword).stream()
                .map(memberMapper::toDomain)
                .toList();
    }

    @Override
    public Member save(Member member) {
        MemberJpaEntity entity = memberMapper.toJpaEntity(member);
        MemberJpaEntity saved = memberJpaRepository.save(entity);
        return memberMapper.toDomain(saved);
    }
}
