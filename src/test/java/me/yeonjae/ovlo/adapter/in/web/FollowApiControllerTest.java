package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.follow.FollowMemberUseCase;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.in.follow.UnfollowMemberUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FollowApiController 슬라이스 — standalone MockMvc(보안 우회).
 *
 * <p>팔로워/팔로잉 목록은 예전에 회원 상세(`MemberResult`)를 통째로 돌려줬다 — 로그인한 회원
 * 누구나 남의 팔로워 목록에서 이메일·생년월일·연락처를 모을 수 있었다. 여기서는 응답에
 * 공개 필드만 남는지와 페이지네이션 파라미터가 전달되는지를 고정한다
 * (인증 여부는 보안 체인 영역이라 여기서 다루지 않는다).
 */
@ExtendWith(MockitoExtension.class)
class FollowApiControllerTest {

    private static final long LOGGED_IN_MEMBER_ID = 42L;

    @Mock private FollowMemberUseCase followMemberUseCase;
    @Mock private UnfollowMemberUseCase unfollowMemberUseCase;
    @Mock private GetFollowQuery getFollowQuery;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FollowApiController controller =
                new FollowApiController(followMemberUseCase, unfollowMemberUseCase, getFollowQuery);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return LOGGED_IN_MEMBER_ID;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("팔로워 목록은 공개 필드만 담고 총계를 함께 준다")
    void followers_returnPublicFieldsAndTotal() throws Exception {
        given(getFollowQuery.getFollowers(7L, 0, 20)).willReturn(PageResult.of(
                List.of(new MemberSummaryResult(1L, "follower", "팔로워", null)), 37L, 0, 20));

        mockMvc.perform(get("/api/v1/follows/followers/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nickname").value("follower"))
                .andExpect(jsonPath("$.content[0].name").value("팔로워"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].contactInfos").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(37))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("페이지·크기 파라미터가 조회로 전달된다")
    void followings_passPaginationParams() throws Exception {
        given(getFollowQuery.getFollowings(7L, 2, 10)).willReturn(PageResult.of(List.of(), 25L, 2, 10));

        mockMvc.perform(get("/api/v1/follows/followings/7").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @DisplayName("관계 확인은 팔로우 중인 ID 만 돌려준다")
    void followingStatus_returnsOnlyFollowedIds() throws Exception {
        given(getFollowQuery.filterFollowing(LOGGED_IN_MEMBER_ID, List.of(2L, 3L))).willReturn(List.of(3L));

        mockMvc.perform(get("/api/v1/follows/following-status").param("memberIds", "2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following[0]").value(3))
                .andExpect(jsonPath("$.following.length()").value(1));
    }
}
