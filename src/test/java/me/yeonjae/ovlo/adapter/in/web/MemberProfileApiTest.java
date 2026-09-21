package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.member.CompleteOnboardingUseCase;
import me.yeonjae.ovlo.application.port.in.member.GetMemberQuery;
import me.yeonjae.ovlo.application.port.in.member.RegisterMemberUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateMemberProfileUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateProfileImageUseCase;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.shared.security.ClientIpResolver;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
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
 * {@code GET /members/{id}} 가 조회자를 공개 범위 판단에 넘기는지 고정한다.
 * 공개 범위 규칙 자체는 {@code MemberQueryServiceTest.GetProfile} 이 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class MemberProfileApiTest {

    private static final long VIEWER_ID = 42L;

    @Mock private RegisterMemberUseCase registerMemberUseCase;
    @Mock private UpdateMemberProfileUseCase updateMemberProfileUseCase;
    @Mock private UpdateProfileImageUseCase updateProfileImageUseCase;
    @Mock private WithdrawMemberUseCase withdrawMemberUseCase;
    @Mock private CompleteOnboardingUseCase completeOnboardingUseCase;
    @Mock private GetMemberQuery getMemberQuery;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private ClientIpResolver clientIpResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MemberApiController controller = new MemberApiController(
                registerMemberUseCase, updateMemberProfileUseCase, updateProfileImageUseCase,
                withdrawMemberUseCase, completeOnboardingUseCase, getMemberQuery,
                rateLimiterService, clientIpResolver);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return VIEWER_ID;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("다른 회원의 프로필은 조회자를 넘겨 받은 공개 범위 그대로 응답한다")
    void othersProfile_passesViewerAndHidesPrivateFields() throws Exception {
        MemberResult publicView = new MemberResult(7L, "kimchi", "김치", "Seoul", null, 1L, null,
                "ACTIVE", MemberRole.MEMBER, "안녕하세요", null, null,
                List.of(), List.of(), List.of(new MemberResult.ContactInfoData("SNS", "@kimchi")));
        given(getMemberQuery.getProfile(new MemberId(7L), new MemberId(VIEWER_ID))).willReturn(publicView);

        mockMvc.perform(get("/api/v1/members/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("kimchi"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andExpect(jsonPath("$.contactInfos[0].value").value("@kimchi"));
    }
}
