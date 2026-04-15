package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

public record MemberCredentials(MemberId memberId, String hashedPassword, MemberRole role) {
}
