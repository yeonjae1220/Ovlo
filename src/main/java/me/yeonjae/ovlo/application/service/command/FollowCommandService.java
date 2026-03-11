package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.FollowCommand;
import me.yeonjae.ovlo.application.port.in.follow.FollowMemberUseCase;
import me.yeonjae.ovlo.application.port.in.follow.UnfollowMemberUseCase;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.domain.follow.exception.FollowException;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FollowCommandService implements FollowMemberUseCase, UnfollowMemberUseCase {

    private final LoadFollowPort loadFollowPort;
    private final SaveFollowPort saveFollowPort;

    public FollowCommandService(LoadFollowPort loadFollowPort, SaveFollowPort saveFollowPort) {
        this.loadFollowPort = loadFollowPort;
        this.saveFollowPort = saveFollowPort;
    }

    @Override
    public void follow(FollowCommand command) {
        MemberId followerId = new MemberId(command.followerId());
        MemberId followeeId = new MemberId(command.followeeId());

        if (loadFollowPort.existsByFollowerAndFollowee(followerId, followeeId)) {
            throw new FollowException("이미 팔로우 중인 회원입니다", FollowException.ErrorType.CONFLICT);
        }

        Follow follow = Follow.create(followerId, followeeId);
        try {
            saveFollowPort.save(follow);
        } catch (DataIntegrityViolationException e) {
            throw new FollowException("이미 팔로우 중인 회원입니다", FollowException.ErrorType.CONFLICT);
        }
    }

    @Override
    public void unfollow(FollowCommand command) {
        MemberId followerId = new MemberId(command.followerId());
        MemberId followeeId = new MemberId(command.followeeId());

        Follow follow = loadFollowPort.findByFollowerAndFollowee(followerId, followeeId)
                .orElseThrow(() -> new FollowException("팔로우 관계를 찾을 수 없습니다", FollowException.ErrorType.NOT_FOUND));

        saveFollowPort.delete(follow);
    }
}
