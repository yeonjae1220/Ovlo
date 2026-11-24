package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityVideoLinksQuery;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 잘못된 요청이 500 이 아니라 4xx 로 나가는지를 실제 컨텍스트(보안 필터·메서드 검증 AOP 포함)로 검증한다.
 * standalone MockMvc 는 class-level {@code @Validated} 의 AOP 검증을 태우지 않아 이 결함을 재현하지 못한다(GLOBAL-PIT-275).
 */
@SpringBootTest
@ActiveProfiles("test")
class RequestErrorMappingTest {

    private static final String VIDEO_LINKS = "/api/v1/universities/7/video-links";

    @Autowired private WebApplicationContext context;
    @MockitoBean private RateLimiterService rateLimiter;
    @MockitoBean private GetUniversityVideoLinksQuery videoLinksQuery;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("대조군: 허용 범위의 값은 200")
    void validRequestPasses() throws Exception {
        given(videoLinksQuery.getVideoLinks(anyLong(), anyInt(), anyInt()))
                .willReturn(new PageResult<>(List.of(), 0, 0, 0, 50, false));

        mvc.perform(get(VIDEO_LINKS).param("size", "50").param("page", "1000"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "범위 밖 값 {0} → 400")
    @ValueSource(strings = {"size=51", "size=0", "page=-1", "page=1001"})
    void outOfRangeQueryParamIsBadRequest(String query) throws Exception {
        String[] pair = query.split("=");

        mvc.perform(get(VIDEO_LINKS).param(pair[0], pair[1]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors." + pair[0]).isNotEmpty())
                .andExpect(content().string(not(containsString("getVideoLinks"))))
                .andExpect(content().string(not(containsString("me.yeonjae"))));

        verify(videoLinksQuery, never()).getVideoLinks(anyLong(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("범위 밖 경로 변수 → 400")
    void outOfRangePathVariableIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/universities/0/video-links"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("카탈로그 검색의 범위 밖 size → 400")
    void catalogOutOfRangeIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/universities/catalog").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @ParameterizedTest(name = "타입이 다른 값 {0} → 400")
    @ValueSource(strings = {"/api/v1/universities/7/video-links?size=abc", "/api/v1/universities/abc/video-links"})
    void typeMismatchIsBadRequest(String uri) throws Exception {
        mvc.perform(get(uri))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("NumberFormatException"))));
    }

    @Test
    @DisplayName("없는 경로 → 404 (500 아님)")
    void unknownPathIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/universities/nope/xyz/zzz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("비로그인 + 공개되지 않은 없는 경로 → 여전히 401 (경로 존재 여부를 알려 주지 않는다)")
    void unknownProtectedPathStaysUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/no-such-resource/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그인한 사용자의 지원하지 않는 메서드 → 405 + Allow 헤더 (500 아님). 비로그인은 보안 계층이 먼저 401 로 막는다")
    void unsupportedMethodIsReported() throws Exception {
        mvc.perform(delete("/api/v1/universities/catalog"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/universities/catalog").with(user("member")).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(header().exists("Allow"));
    }

    @Test
    @DisplayName("깨진 JSON 본문 → 400 (500 아님), 파서 내부 메시지를 내보내지 않는다")
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(content().string(not(containsString("JSON parse error"))))
                .andExpect(content().string(not(containsString("jackson"))));
    }

    @Test
    @DisplayName("지원하지 않는 Content-Type → 415 (500 아님)")
    void unsupportedMediaTypeIsReported() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("REQUEST_ERROR"));
    }
}
