package me.yeonjae.ovlo.application.port.in.moderation;

import me.yeonjae.ovlo.application.dto.result.BlockedMemberPageResult;

public interface GetBlockedMembersQuery {
    /** before 는 이전 페이지의 nextBefore (회원 ID 내림차순 키셋). */
    BlockedMemberPageResult blockedMembers(long actorId, Long before);

    /** 내가 target 을 차단했는지 (프로필 화면의 차단/해제 버튼). */
    boolean isBlockedByMe(long actorId, long targetId);
}
