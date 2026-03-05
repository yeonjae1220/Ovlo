package me.yeonjae.ovlo.application.port.out.member;

import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Optional;

public interface LoadMemberPort {
    Optional<Member> findById(MemberId memberId);
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
}
