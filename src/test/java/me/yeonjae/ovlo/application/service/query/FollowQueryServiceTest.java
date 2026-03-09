package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.follow.model.FollowId;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FollowQueryServiceTest {

    @Mock LoadFollowPort loadFollowPort;
    @Mock LoadMemberPort loadMemberPort;

    @InjectMocks
    FollowQueryService service;

    private Member follower;
    private Member followee;

    @BeforeEach
    void setUp() {
        follower = Member.create("follower", "팔로워", "Seoul",
                new Email("follower@example.com"),
                new Password("hashed"),
                new UniversityId(1L),
                new Major("CS", DegreeType.BACHELOR, 1));
        follower.assignId(new MemberId(1L));

        followee = Member.create("followee", "팔로위", "Busan",
                new Email("followee@example.com"),
                new Password("hashed"),
                new UniversityId(1L),
                new Major("CS", DegreeType.BACHELOR, 1));
        followee.assignId(new MemberId(2L));
    }

    @Nested
    @DisplayName("getFollowers()")
    class GetFollowers {

        @Test
        @DisplayName("나를 팔로우하는 사람 목록을 조회할 수 있다")
        void shouldGetFollowers() {
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findFollowersByFolloweeId(any())).willReturn(List.of(follow));
            given(loadMemberPort.findById(new MemberId(1L))).willReturn(Optional.of(follower));

            List<MemberResult> results = service.getFollowers(2L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(1L); // follower의 memberId
        }
    }

    @Nested
    @DisplayName("getFollowings()")
    class GetFollowings {

        @Test
        @DisplayName("내가 팔로우하는 사람 목록을 조회할 수 있다")
        void shouldGetFollowings() {
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findFollowingsByFollowerId(any())).willReturn(List.of(follow));
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(followee));

            List<MemberResult> results = service.getFollowings(1L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(2L); // followee의 memberId
        }
    }
}
