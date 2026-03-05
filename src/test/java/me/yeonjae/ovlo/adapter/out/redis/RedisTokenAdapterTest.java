package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.model.MemberId;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RedisTokenAdapterTest {

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
    private RedisTokenAdapter adapter;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private MemberId memberId;
    private String refreshToken;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        memberId = new MemberId(1L);
        refreshToken = "test-refresh-token";
        expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("세션을 저장하고 memberId로 조회할 수 있다")
    void shouldSaveAndFindByMemberId() {
        AuthSession session = AuthSession.create(memberId, refreshToken, expiresAt);

        adapter.save(session);
        Optional<AuthSession> found = adapter.findByMemberId(memberId);

        assertThat(found).isPresent();
        assertThat(found.get().getMemberId()).isEqualTo(memberId);
        assertThat(found.get().getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("세션을 저장하고 refreshToken으로 조회할 수 있다")
    void shouldSaveAndFindByRefreshToken() {
        AuthSession session = AuthSession.create(memberId, refreshToken, expiresAt);

        adapter.save(session);
        Optional<AuthSession> found = adapter.findByRefreshToken(refreshToken);

        assertThat(found).isPresent();
        assertThat(found.get().getMemberId()).isEqualTo(memberId);
    }

    @Test
    @DisplayName("존재하지 않는 memberId로 조회하면 빈 Optional을 반환한다")
    void shouldReturnEmpty_whenMemberNotFound() {
        Optional<AuthSession> found = adapter.findByMemberId(new MemberId(999L));
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("세션을 삭제하면 이후 조회가 빈 Optional을 반환한다")
    void shouldDeleteSession() {
        AuthSession session = AuthSession.create(memberId, refreshToken, expiresAt);
        adapter.save(session);

        adapter.delete(memberId);

        assertThat(adapter.findByMemberId(memberId)).isEmpty();
        assertThat(adapter.findByRefreshToken(refreshToken)).isEmpty();
    }

    @Test
    @DisplayName("동일 memberId로 저장하면 이전 세션을 덮어쓴다")
    void shouldOverwrite_whenSavingWithSameMemberId() {
        adapter.save(AuthSession.create(memberId, "first-token", expiresAt));
        adapter.save(AuthSession.create(memberId, "second-token", expiresAt));

        Optional<AuthSession> found = adapter.findByMemberId(memberId);
        assertThat(found).isPresent();
        assertThat(found.get().getRefreshToken()).isEqualTo("second-token");
    }
}
