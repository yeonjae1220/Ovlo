package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.FollowResult;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FollowQueryServiceTest {

    @Mock LoadFollowPort loadFollowPort;

    @InjectMocks
    FollowQueryService service;

    @Nested
    @DisplayName("getFollowers()")
    class GetFollowers {

        @Test
        @DisplayName("나를 팔로우하는 사람 목록을 조회할 수 있다")
        void shouldGetFollowers() {
            MemberId followeeId = new MemberId(2L);
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), followeeId);
            given(loadFollowPort.findFollowersByFolloweeId(any())).willReturn(List.of(follow));

            List<FollowResult> results = service.getFollowers(2L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).followerId()).isEqualTo(1L);
            assertThat(results.get(0).followeeId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("getFollowings()")
    class GetFollowings {

        @Test
        @DisplayName("내가 팔로우하는 사람 목록을 조회할 수 있다")
        void shouldGetFollowings() {
            MemberId followerId = new MemberId(1L);
            Follow follow = Follow.restore(new FollowId(1L), followerId, new MemberId(2L));
            given(loadFollowPort.findFollowingsByFollowerId(any())).willReturn(List.of(follow));

            List<FollowResult> results = service.getFollowings(1L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).followerId()).isEqualTo(1L);
            assertThat(results.get(0).followeeId()).isEqualTo(2L);
        }
    }
}
