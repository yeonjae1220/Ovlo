package me.yeonjae.ovlo.application.port.out.member;

import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LoadMemberPort {
    Optional<Member> findById(MemberId memberId);
    List<Member> findAllByIds(List<MemberId> ids);
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    List<Member> searchByNickname(String keyword);
    Page<Member> findAll(Pageable pageable);
    long count();
}
