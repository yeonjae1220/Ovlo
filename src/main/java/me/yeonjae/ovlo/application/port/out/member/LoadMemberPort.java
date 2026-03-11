package me.yeonjae.ovlo.application.port.out.member;

import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.List;
import java.util.Optional;

public interface LoadMemberPort {
    Optional<Member> findById(MemberId memberId);
    List<Member> findAllByIds(List<MemberId> ids);
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    List<Member> searchByNickname(String keyword);
}
