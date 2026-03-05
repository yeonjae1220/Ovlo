package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.member.model.MemberId;

public record MemberCredentials(MemberId memberId, String hashedPassword) {
}
