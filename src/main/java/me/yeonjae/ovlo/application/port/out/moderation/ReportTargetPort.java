package me.yeonjae.ovlo.application.port.out.moderation;

import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;

import java.util.OptionalLong;

public interface ReportTargetPort {
    /**
     * 신고자가 볼 수 있는 대상이면 그 작성자 ID 를 돌려준다.
     * 지워졌거나, 숨겨졌거나, 신고자가 참여하지 않은 채팅방의 메시지면 비어 있다.
     */
    OptionalLong findAuthorVisibleTo(ReportTargetType type, long targetId, long reporterId);

    /** 게시글·댓글·채팅 메시지를 내린다. 회원은 대상이 아니다. */
    void removeContent(ReportTargetType type, long targetId);
}
