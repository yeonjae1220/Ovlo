package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
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
        @DisplayName("요청한 페이지만 조회하고, 다른 회원에게 보여도 되는 필드만 담는다")
        void shouldReturnRequestedPageWithSummaryFieldsOnly() {
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findFollowersByFolloweeId(new MemberId(2L), 0, 20)).willReturn(List.of(follow));
            given(loadFollowPort.countFollowersByFolloweeId(new MemberId(2L))).willReturn(37L);
            given(loadMemberPort.findAllByIds(any())).willReturn(List.of(follower));

            PageResult<MemberSummaryResult> page = service.getFollowers(2L, 0, 20);

            assertThat(page.content())
                    .containsExactly(new MemberSummaryResult(1L, "follower", "팔로워", null));
            assertThat(page.totalElements()).isEqualTo(37L);
            assertThat(page.hasNext()).isTrue();
        }

        @Test
        @DisplayName("페이지 번호는 offset 으로 환산해 조회한다")
        void shouldTranslatePageToOffset() {
            given(loadFollowPort.findFollowersByFolloweeId(new MemberId(2L), 20, 10)).willReturn(List.of());
            given(loadFollowPort.countFollowersByFolloweeId(new MemberId(2L))).willReturn(25L);

            PageResult<MemberSummaryResult> page = service.getFollowers(2L, 2, 10);

            assertThat(page.content()).isEmpty();
            assertThat(page.page()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getFollowings()")
    class GetFollowings {

        @Test
        @DisplayName("요청한 페이지만 조회하고, 다른 회원에게 보여도 되는 필드만 담는다")
        void shouldReturnRequestedPageWithSummaryFieldsOnly() {
            Follow follow = Follow.restore(new FollowId(1L), new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findFollowingsByFollowerId(new MemberId(1L), 0, 20)).willReturn(List.of(follow));
            given(loadFollowPort.countFollowingsByFollowerId(new MemberId(1L))).willReturn(1L);
            given(loadMemberPort.findAllByIds(any())).willReturn(List.of(followee));

            PageResult<MemberSummaryResult> page = service.getFollowings(1L, 0, 20);

            assertThat(page.content())
                    .containsExactly(new MemberSummaryResult(2L, "followee", "팔로위", null));
            assertThat(page.totalElements()).isEqualTo(1L);
            assertThat(page.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("filterFollowing() — 관계 확인")
    class FilterFollowing {

        @Test
        @DisplayName("후보 중 실제로 팔로우 중인 회원 ID 만 돌려준다 (회원 정보는 조회하지 않는다)")
        void shouldReturnOnlyFollowedIds() {
            given(loadFollowPort.findFollowingIdsIn(new MemberId(1L), List.of(new MemberId(2L), new MemberId(3L))))
                    .willReturn(List.of(new MemberId(3L)));

            List<Long> following = service.filterFollowing(1L, List.of(2L, 3L));

            assertThat(following).containsExactly(3L);
        }

        @Test
        @DisplayName("후보가 비어 있으면 조회 없이 빈 목록")
        void shouldShortCircuitOnEmptyCandidates() {
            assertThat(service.filterFollowing(1L, List.of())).isEmpty();
        }
    }
}
