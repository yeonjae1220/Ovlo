package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.exception.GlobalExceptionHandler;
import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import me.yeonjae.ovlo.shared.security.ClientIpResolver;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UniversityApiController 슬라이스 — standalone MockMvc(보안 우회).
 * 공개 읽기 엔드포인트가 IP 기준 search rate limit을 거치는지(특히 /{id} 순차 enumeration 완화)를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UniversityApiControllerTest {

    private static final String CLIENT_IP = "203.0.113.7";

    @Mock private SearchUniversityQuery searchUniversityQuery;
    @Mock private GetUniversityQuery getUniversityQuery;
    @Mock private SearchUniversityCatalogQuery searchCatalogQuery;
    @Mock private RateLimiterService rateLimiter;
    @Mock private ClientIpResolver clientIpResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UniversityApiController controller = new UniversityApiController(
                searchUniversityQuery, getUniversityQuery, searchCatalogQuery, rateLimiter, clientIpResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("단건 조회: rate limit 체크 후 해석된 IP로 검사하고 유스케이스에 위임한다")
    void getById_checksSearchRateThenDelegates() throws Exception {
        given(clientIpResolver.resolve(any())).willReturn(CLIENT_IP);

        mockMvc.perform(get("/api/v1/universities/5"))
                .andExpect(status().isOk());

        verify(rateLimiter).checkSearchRate(CLIENT_IP);
        verify(getUniversityQuery).getById(new UniversityId(5L));
    }

    @Test
    @DisplayName("단건 조회: search rate limit 초과 → 429, 유스케이스 미호출 (enumeration 스크래핑 차단)")
    void getById_rateLimited_returns429AndSkipsQuery() throws Exception {
        given(clientIpResolver.resolve(any())).willReturn(CLIENT_IP);
        willThrow(new TooManyRequestsException("요청이 너무 많습니다"))
                .given(rateLimiter).checkSearchRate(CLIENT_IP);

        mockMvc.perform(get("/api/v1/universities/5"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));

        verify(getUniversityQuery, never()).getById(any());
    }

    @Test
    @DisplayName("대학 검색(/): IP 기준 search rate limit을 거친다")
    void search_checksSearchRate() throws Exception {
        given(clientIpResolver.resolve(any())).willReturn(CLIENT_IP);

        mockMvc.perform(get("/api/v1/universities").param("keyword", "Tokyo"))
                .andExpect(status().isOk());

        verify(rateLimiter).checkSearchRate(CLIENT_IP);
    }

    @Test
    @DisplayName("통합 카탈로그 검색(/catalog): IP 기준 search rate limit을 거친다")
    void searchCatalog_checksSearchRate() throws Exception {
        given(clientIpResolver.resolve(any())).willReturn(CLIENT_IP);

        mockMvc.perform(get("/api/v1/universities/catalog").param("keyword", "Seoul"))
                .andExpect(status().isOk());

        verify(rateLimiter).checkSearchRate(CLIENT_IP);
    }

    @Test
    @DisplayName("카탈로그 국가 목록(/catalog/countries): IP 기준 search rate limit을 거친다")
    void getCatalogCountries_checksSearchRate() throws Exception {
        given(clientIpResolver.resolve(any())).willReturn(CLIENT_IP);

        mockMvc.perform(get("/api/v1/universities/catalog/countries"))
                .andExpect(status().isOk());

        verify(rateLimiter).checkSearchRate(CLIENT_IP);
    }
}
