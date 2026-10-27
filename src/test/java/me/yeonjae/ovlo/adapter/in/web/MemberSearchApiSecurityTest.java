package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
import me.yeonjae.ovlo.application.port.in.member.GetMemberQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 회원 검색이 다시 익명에게 열리지 않도록 실제 {@code securityFilterChain} 을 태워 고정한다.
 *
 * <p>2026-09-17 운영 실측: {@code GET /api/v1/members/search} 가 permitAll 이었고 응답에
 * 이메일·생년월일·연락처가 담겼다. 닉네임 부분 일치에 결과 수 제한도 없어서, 로그인 없이
 * 한 글자씩 조회하면 회원 전체의 개인정보를 가져갈 수 있었다. 가입 화면이 닉네임 중복 확인에
 * 이 엔드포인트를 쓰느라 열어둔 것이라, 중복 확인은 사용 가능 여부만 주는
 * {@code /check-nickname} 으로 분리했다.
 *
 * <p>standalone MockMvc 컨트롤러 테스트는 보안 필터를 건너뛰므로 이 회귀를 잡지 못한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class MemberSearchApiSecurityTest {

    private static final long LOGGED_IN_MEMBER_ID = 42L;

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private GetMemberQuery getMemberQuery;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("비로그인 회원 검색은 401 이고 조회까지 도달하지 않는다")
    void search_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/members/search").param("nickname", "a"))
                .andExpect(status().isUnauthorized());

        verify(getMemberQuery, never()).searchByNickname(anyString());
    }

    @Test
    @DisplayName("로그인 회원 검색 응답에는 공개 필드만 있고 이메일·생년월일·연락처는 없다")
    void search_authenticated_returnsPublicFieldsOnly() throws Exception {
        given(getMemberQuery.searchByNickname("kim"))
                .willReturn(List.of(new MemberSummaryResult(7L, "kimchi", "김치", null)));

        mockMvc.perform(get("/api/v1/members/search").param("nickname", "kim")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                LOGGED_IN_MEMBER_ID, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nickname").value("kimchi"))
                .andExpect(jsonPath("$[0].name").value("김치"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$[0].contactInfos").doesNotExist());
    }

    @Test
    @DisplayName("닉네임 중복 확인은 비로그인으로 호출할 수 있고 사용 가능 여부만 준다")
    void checkNickname_anonymous_returnsAvailabilityOnly() throws Exception {
        given(getMemberQuery.isNicknameAvailable("kim")).willReturn(false);

        mockMvc.perform(get("/api/v1/members/check-nickname").param("nickname", "kim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("빈 닉네임은 조회 없이 사용할 수 없다고 답한다")
    void checkNickname_blank_isUnavailableWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/v1/members/check-nickname").param("nickname", "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        verify(getMemberQuery, never()).isNicknameAvailable(anyString());
    }
}
