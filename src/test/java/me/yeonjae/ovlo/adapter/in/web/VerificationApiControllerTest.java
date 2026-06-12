package me.yeonjae.ovlo.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.yeonjae.ovlo.application.dto.command.ConfirmSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.command.RequestSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.VerificationRequestResult;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.port.in.verification.ConfirmSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.in.verification.RequestSchoolEmailVerificationUseCase;
import me.yeonjae.ovlo.shared.exception.GlobalExceptionHandler;
import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VerificationApiController 슬라이스 — standalone MockMvc(보안 우회).
 * 요청 매핑/검증/유스케이스 위임/rate limit 호출을 격리 검증한다.
 * (보안 필터·JWT는 통합 영역으로 분리; @AuthenticationPrincipal은 고정 멤버를 주입하는 리졸버로 대체)
 */
@ExtendWith(MockitoExtension.class)
class VerificationApiControllerTest {

    private static final long MEMBER_ID = 42L;

    @Mock private RequestSchoolEmailVerificationUseCase requestUseCase;
    @Mock private ConfirmSchoolEmailVerificationUseCase confirmUseCase;
    @Mock private GetMyVerificationStatusQuery statusQuery;
    @Mock private RateLimiterService rateLimiter;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        VerificationApiController controller =
                new VerificationApiController(requestUseCase, confirmUseCase, statusQuery, rateLimiter);

        // @AuthenticationPrincipal Long → 고정 멤버 ID 주입
        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                          org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                          org.springframework.web.context.request.NativeWebRequest webRequest,
                                          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                return MEMBER_ID;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("코드 발송: 유효 요청 → 200, rate limit 체크 후 유스케이스에 인증 멤버ID로 위임")
    void requestCode_valid_delegatesWithAuthenticatedMember() throws Exception {
        given(requestUseCase.request(any()))
                .willReturn(new VerificationRequestResult("s*****t@univ.edu", 600L));

        mockMvc.perform(post("/api/v1/verification/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityId\":1,\"schoolEmail\":\"student@univ.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedEmail").value("s*****t@univ.edu"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));

        verify(rateLimiter).checkEmailVerificationRate(MEMBER_ID);

        ArgumentCaptor<RequestSchoolEmailVerificationCommand> captor =
                ArgumentCaptor.forClass(RequestSchoolEmailVerificationCommand.class);
        verify(requestUseCase).request(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().universityId()).isEqualTo(1L);
        assertThat(captor.getValue().schoolEmail()).isEqualTo("student@univ.edu");
    }

    @Test
    @DisplayName("코드 발송: universityId 누락 → 400, 유스케이스 미호출")
    void requestCode_missingUniversityId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/verification/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolEmail\":\"student@univ.edu\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(requestUseCase, never()).request(any());
        verify(rateLimiter, never()).checkEmailVerificationRate(any());
    }

    @Test
    @DisplayName("코드 발송: 이메일 형식 오류 → 400")
    void requestCode_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/verification/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityId\":1,\"schoolEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(requestUseCase, never()).request(any());
    }

    @Test
    @DisplayName("코드 발송: rate limit 초과 → 429, 유스케이스 미호출")
    void requestCode_rateLimited_returns429() throws Exception {
        org.mockito.BDDMockito.willThrow(new TooManyRequestsException("요청이 너무 많습니다"))
                .given(rateLimiter).checkEmailVerificationRate(MEMBER_ID);

        mockMvc.perform(post("/api/v1/verification/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"universityId\":1,\"schoolEmail\":\"student@univ.edu\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));

        verify(requestUseCase, never()).request(any());
    }

    @Test
    @DisplayName("코드 확인: 6자리 코드 → 200, 인증 멤버ID로 위임하고 현황 반환")
    void confirmCode_valid_returnsStatus() throws Exception {
        given(confirmUseCase.confirm(any()))
                .willReturn(new VerificationStatusResult("STUDENT",
                        List.of(new VerificationStatusResult.VerifiedUniversity(1L, "s*****t@univ.edu", "2026-06-12T00:00:00Z"))));

        mockMvc.perform(post("/api/v1/verification/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustLevel").value("STUDENT"))
                .andExpect(jsonPath("$.verifiedUniversities[0].universityId").value(1));

        ArgumentCaptor<ConfirmSchoolEmailVerificationCommand> captor =
                ArgumentCaptor.forClass(ConfirmSchoolEmailVerificationCommand.class);
        verify(confirmUseCase).confirm(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().code()).isEqualTo("123456");
    }

    @Test
    @DisplayName("코드 확인: 6자리 숫자가 아니면 → 400, 유스케이스 미호출")
    void confirmCode_invalidPattern_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/verification/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"abc\"}"))
                .andExpect(status().isBadRequest());

        verify(confirmUseCase, never()).confirm(any());
    }

    @Test
    @DisplayName("내 인증 현황: 200, 인증 멤버ID로 조회")
    void myStatus_returnsStatusForAuthenticatedMember() throws Exception {
        given(statusQuery.getByMemberId(MEMBER_ID))
                .willReturn(new VerificationStatusResult("UNVERIFIED", List.of()));

        mockMvc.perform(get("/api/v1/verification/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustLevel").value("UNVERIFIED"))
                .andExpect(jsonPath("$.verifiedUniversities").isEmpty());

        verify(statusQuery).getByMemberId(MEMBER_ID);
    }
}
