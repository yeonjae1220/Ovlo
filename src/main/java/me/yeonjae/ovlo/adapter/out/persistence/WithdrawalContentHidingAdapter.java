package me.yeonjae.ovlo.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import me.yeonjae.ovlo.adapter.out.persistence.repository.CommentJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.FollowJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MessageJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PostJpaRepository;
import me.yeonjae.ovlo.application.port.out.member.HideContentByWithdrawnMemberPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WithdrawalContentHidingAdapter implements HideContentByWithdrawnMemberPort {

    private final PostJpaRepository postJpaRepository;
    private final CommentJpaRepository commentJpaRepository;
    private final MessageJpaRepository messageJpaRepository;
    private final FollowJpaRepository followJpaRepository;

    @Override
    @Transactional
    public void hideAllContentByMember(Long memberId) {
        postJpaRepository.hideAllByAuthorId(memberId);
        commentJpaRepository.hideAllByAuthorId(memberId);
        messageJpaRepository.hideAllBySenderId(memberId);
        followJpaRepository.hideAllByMemberId(memberId);
    }
}
