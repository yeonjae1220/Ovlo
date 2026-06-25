package me.yeonjae.ovlo.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 실제 {@code adminFilterChain} + 실제 Redis(Testcontainers)로 검증하는 lockout end-to-end.
 *
 * <p>{@link AdminLoginLockoutIntegrationTest}(서비스 mock — wiring 검증)와 달리, 여기서는
 * Redis 카운팅까지 mock 없이 태운다: 같은 IP로 {@code ip-max-attempts}(기본 5)회 로그인을
 * 연속 실패시키면 다음 요청이 비밀번호 검증 전에 429로 차단되는지를 확인한다. SnapGuide의
 * 임베디드 Redis 통합 테스트와 대칭. Docker가 필요하므로 {@code @Tag("integration")}로 분리
 * (평소 {@code ./gradlew test}에선 제외, {@code ./gradlew integrationTest}로 실행).
 *
 * <p>IP는 {@link ClientIpResolver}가 신뢰 프록시(127.0.0.1)에서 온 X-Real-IP를 신뢰해
 * 해석하므로, 헤더로 합성 클라이언트 IP를 주입한다.
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AdminLoginLockoutRedisIntegrationTest {

    private static final String CLIENT_IP = "198.51.100.77";
    private static final String OTHER_IP = "203.0.113.5";
    private static final String ADMIN = "admin@test.example.com";

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("같은 IP에서 5회 연속 실패하면 6회째 POST는 429 + Retry-After 로 차단된다")
    void fiveFailures_sixthBlockedWith429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/admin/login").with(csrf())
                            .header("X-Real-IP", CLIENT_IP)
                            .param("username", ADMIN)
                            .param("password", "wrong-" + i))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/login?error"));
        }

        mockMvc.perform(post("/admin/login").with(csrf())
                        .header("X-Real-IP", CLIENT_IP)
                        .param("username", ADMIN)
                        .param("password", "wrong-again"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    @DisplayName("한 IP의 lockout은 다른 IP에 영향을 주지 않는다 (per-IP 격리)")
    void lockoutIsScopedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/admin/login").with(csrf())
                            .header("X-Real-IP", CLIENT_IP)
                            .param("username", ADMIN)
                            .param("password", "x" + i))
                    .andExpect(status().is3xxRedirection());
        }

        // 다른 IP는 락이 없으므로 정상 실패 처리(?error)되고 429가 아니다
        // (계정 임계값 20 > 누적 6회라 계정 차원 락도 걸리지 않는다)
        mockMvc.perform(post("/admin/login").with(csrf())
                        .header("X-Real-IP", OTHER_IP)
                        .param("username", ADMIN)
                        .param("password", "y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }
}
