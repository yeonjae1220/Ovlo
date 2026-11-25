package me.yeonjae.ovlo.application.port.out.moderation;

import me.yeonjae.ovlo.application.dto.result.BlockedMemberResult;

import java.util.List;
import java.util.Set;

public interface MemberBlockPort {
    void block(long actorId, long targetId);

    void unblock(long actorId, long targetId);

    /** actor 가 target 을 차단했으면 true (한 방향). */
    boolean blocked(long actorId, long targetId);

    /** 어느 한쪽이라도 상대를 차단했으면 true. */
    boolean blockedEitherWay(long memberA, long memberB);

    /** memberId 가 차단했거나 memberId 를 차단한 회원 전부. 피드·댓글에서 서로 숨길 때 쓴다. */
    Set<Long> relatedMemberIds(long memberId);

    /** actor 가 차단한 회원을 회원 ID 내림차순으로 limit 개까지. */
    List<BlockedMemberResult> blockedBy(long actorId, Long before, int limit);
}
