package me.yeonjae.ovlo.application.port.out.post;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.post.model.ReactionType;

public interface SavePostPort {

    Post save(Post post);

    /**
     * 한 회원의 반응(좋아요/싫어요)을 upsert 한다(A안).
     *
     * <p>반응은 회원 1명당 {@code post_reaction} 1행({@code (post_id, member_id)} PK)으로 관리되므로
     * 서로 다른 회원의 동시 반응은 서로 다른 행이라 충돌하지 않는다. 전체 애그리거트(Post)를 다시
     * 저장하지 않고 해당 회원 행만 삽입/전환하며, 비정규화 카운트는 원자적 UPDATE 로 함께 갱신한다.
     * 이미 동일한 반응이면 무연산(idempotent)이고, 반대 반응이면 전환하며 카운트를 −1/+1 조정한다.
     */
    void upsertReaction(PostId postId, MemberId memberId, ReactionType type);

    /**
     * 한 회원의 반응을 제거한다(A안). 해당 회원 행만 삭제하고 비정규화 카운트를 원자적으로 감소시킨다.
     * 반응이 없으면 무연산이다(상위 도메인에서 NOT_FOUND 검증을 담당).
     */
    void removeReaction(PostId postId, MemberId memberId);
}
