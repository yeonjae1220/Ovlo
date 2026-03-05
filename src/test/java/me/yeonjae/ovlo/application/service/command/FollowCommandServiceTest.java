package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.FollowCommand;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.domain.follow.exception.FollowException;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.follow.model.FollowId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowCommandServiceTest {

    @Mock LoadFollowPort loadFollowPort;
    @Mock SaveFollowPort saveFollowPort;

    @InjectMocks
    FollowCommandService service;

    @Nested
    @DisplayName("follow()")
    class FollowTest {

        @Test
        @DisplayName("팔로우할 수 있다")
        void shouldFollow() {
            FollowCommand command = new FollowCommand(1L, 2L);
            given(loadFollowPort.existsByFollowerAndFollowee(any(), any())).willReturn(false);
            given(saveFollowPort.save(any())).willReturn(
                    Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L)));

            service.follow(command);

            verify(saveFollowPort).save(any());
        }

        @Test
        @DisplayName("이미 팔로우 중인 회원을 팔로우하면 예외가 발생한다")
        void shouldThrow_whenAlreadyFollowing() {
            FollowCommand command = new FollowCommand(1L, 2L);
            given(loadFollowPort.existsByFollowerAndFollowee(any(), any())).willReturn(true);

            assertThatThrownBy(() -> service.follow(command))
                    .isInstanceOf(FollowException.class)
                    .hasMessageContaining("이미 팔로우 중인 회원입니다");
        }
    }

    @Nested
    @DisplayName("unfollow()")
    class UnfollowTest {

        @Test
        @DisplayName("팔로우를 취소할 수 있다")
        void shouldUnfollow() {
            FollowCommand command = new FollowCommand(1L, 2L);
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findByFollowerAndFollowee(any(), any())).willReturn(Optional.of(follow));

            service.unfollow(command);

            verify(saveFollowPort).delete(follow);
        }

        @Test
        @DisplayName("팔로우 관계가 없으면 예외가 발생한다")
        void shouldThrow_whenFollowNotFound() {
            FollowCommand command = new FollowCommand(1L, 2L);
            given(loadFollowPort.findByFollowerAndFollowee(any(), any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.unfollow(command))
                    .isInstanceOf(FollowException.class)
                    .hasMessageContaining("팔로우 관계를 찾을 수 없습니다");
        }
    }
}
