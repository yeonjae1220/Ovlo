package me.yeonjae.ovlo.application.port.in.moderation;

public interface BlockMemberUseCase {
    /** 이미 차단했으면 아무 일도 하지 않는다. 서로의 팔로우 관계를 끊는다. */
    void block(long actorId, long targetId);

    void unblock(long actorId, long targetId);
}
