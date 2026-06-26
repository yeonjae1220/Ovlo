package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.university.GetUniversityQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityCatalogQuery;
import me.yeonjae.ovlo.application.port.in.university.SearchUniversityQuery;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UniversityApiController 슬라이스 — standalone MockMvc(보안 우회).
 * 공개 read 엔드포인트(검색·단건·국가목록)가 IP 기반 rate limit 을 호출하고,
 * 한도 초과 시 429 로 응답하는지 격리 검증한다. id 순차 enumeration 으로 전체
 * 대학 마스터를 긁어가는 크롤링을 완화하기 위한 회귀 가드.
 */
@ExtendWith(MockitoExtension.class)
class UniversityApiControllerRateLimitTest {

    @Mock private SearchUniversityQuery searchUniversityQuery;
    @Mock private GetUniversityQuery getUniversityQuery;
    @Mock private SearchUniversityCatalogQuery searchCatalogQuery;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private ClientIpResolver clientIpResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UniversityApiController controller = new UniversityApiController(
                searchUniversityQuery, getUniversityQuery, searchCatalogQuery,
                rateLimiterService, clientIpResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        given(clientIpResolver.resolve(any())).willReturn("203.0.113.7");
    }

    @Test
    @DisplayName("단건 조회는 rate limit 을 호출한다")
    void getById_checksRateLimit() throws Exception {
        mockMvc.perform(get("/api/v1/universities/{id}", 1L))
                .andExpect(status().isOk());
        verify(rateLimiterService).checkSearchRate("203.0.113.7");
    }

    @Test
    @DisplayName("기본 검색은 rate limit 을 호출한다")
    void search_checksRateLimit() throws Exception {
        mockMvc.perform(get("/api/v1/universities"))
                .andExpect(status().isOk());
        verify(rateLimiterService).checkSearchRate("203.0.113.7");
    }

    @Test
    @DisplayName("국가 목록은 rate limit 을 호출한다")
    void catalogCountries_checksRateLimit() throws Exception {
        given(searchCatalogQuery.getCountries()).willReturn(List.of());
        mockMvc.perform(get("/api/v1/universities/catalog/countries"))
                .andExpect(status().isOk());
        verify(rateLimiterService).checkSearchRate("203.0.113.7");
    }

    @Test
    @DisplayName("한도 초과 시 단건 조회는 429 로 차단된다(enumeration 완화)")
    void getById_overLimit_returns429() throws Exception {
        doThrow(new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요"))
                .when(rateLimiterService).checkSearchRate(anyString());

        mockMvc.perform(get("/api/v1/universities/{id}", 1L))
                .andExpect(status().isTooManyRequests());
    }
}
