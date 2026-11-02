package me.yeonjae.ovlo.shared.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * GLOBAL-PIT-156 고정 — {@code sendError()} 뒤에 오는 {@code /error} ERROR 디스패치가 메인 체인의
 * catch-all {@code authenticated()} 에 다시 걸리면 원래 상태(403·400)가 401 로 위장된다.
 *
 * <p>실제로 admin 로그인 POST 의 CSRF 거부(403)가 운영에서 본문 없는 401 로 보여 원인 추적에
 * 한 시간이 걸렸다. MockMvc 는 에러 페이지 디스패치를 직접 수행하지 않으므로, 컨테이너가 하는
 * 것과 같은 ERROR 디스패치 요청을 만들어 {@code /error} 로 보내 상태가 보존되는지 본다.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigErrorDispatchTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("비인증 ERROR 디스패치(/error, 원 상태 403)는 401 이 아니라 403 을 그대로 돌려준다")
    void errorDispatchKeepsOriginalStatusInsteadOf401() throws Exception {
        mockMvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
                    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/admin/login");
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @DisplayName("ERROR 디스패치가 아닌 보통 비인증 요청은 여전히 401 로 막힌다 — permitAll 이 새지 않는다")
    void normalUnauthenticatedRequestStillGets401() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/error"))
                .andExpect(status().isUnauthorized());
    }
}
