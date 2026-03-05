package me.yeonjae.ovlo.application.port.out.member;

import me.yeonjae.ovlo.domain.member.model.Member;

public interface SaveMemberPort {
    Member save(Member member);
}
