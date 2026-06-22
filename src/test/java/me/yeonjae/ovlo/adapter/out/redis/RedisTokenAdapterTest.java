package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.shared.security.TokenHashUtil;
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
import java.util.Set;

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
        // 세션에는 평문이 아닌 SHA-256 해시가 보관된다
        assertThat(found.get().getRefreshToken()).isEqualTo(TokenHashUtil.sha256(refreshToken));
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
    @DisplayName("동일 세션을 rotate 후 저장하면 새 토큰으로 갱신된다")
    void shouldUpdateToken_whenSavingRotatedSession() {
        AuthSession session = AuthSession.create(memberId, "first-token", expiresAt);
        adapter.save(session);

        session.rotate("second-token", Instant.now().plus(7, ChronoUnit.DAYS));
        adapter.save(session);

        Optional<AuthSession> found = adapter.findByMemberId(memberId);
        assertThat(found).isPresent();
        assertThat(found.get().getRefreshToken()).isEqualTo(TokenHashUtil.sha256("second-token"));
    }

    @Test
    @DisplayName("refreshToken은 평문이 아닌 SHA-256 해시로 Redis에 저장된다 (GLOBAL-PIT-001)")
    void shouldStoreRefreshTokenHashedNotPlaintext() {
        AuthSession session = AuthSession.create(memberId, refreshToken, expiresAt);
        adapter.save(session);

        String hashed = TokenHashUtil.sha256(refreshToken);

        // 1) 역인덱스 키는 평문이 아닌 해시 기반이어야 한다
        assertThat(redisTemplate.hasKey("auth:token:" + refreshToken)).isFalse();
        assertThat(redisTemplate.hasKey("auth:token:" + hashed)).isTrue();

        // 2) 어떤 Redis 키에도 평문 토큰이 등장하지 않는다
        Set<String> allKeys = redisTemplate.keys("*");
        assertThat(allKeys).isNotNull();
        assertThat(allKeys).noneMatch(k -> k.contains(refreshToken));

        // 3) 세션 Hash의 refreshToken 필드도 해시값이어야 한다
        Set<String> sessionKeys = redisTemplate.keys("auth:session:*");
        assertThat(sessionKeys).hasSize(1);
        Object storedField = redisTemplate.opsForHash()
                .get(sessionKeys.iterator().next(), "refreshToken");
        assertThat(storedField).isEqualTo(hashed);
    }

    @Test
    @DisplayName("토큰 rotation 시 구 토큰 해시 역인덱스가 삭제된다")
    void shouldDeleteOldTokenIndex_onRotation() {
        AuthSession session = AuthSession.create(memberId, "old-token", expiresAt);
        adapter.save(session);

        session.rotate("new-token", Instant.now().plus(7, ChronoUnit.DAYS));
        adapter.save(session);

        // 구 토큰으로는 더 이상 조회되지 않고, 신 토큰으로 조회된다
        assertThat(adapter.findByRefreshToken("old-token")).isEmpty();
        assertThat(adapter.findByRefreshToken("new-token")).isPresent();
        assertThat(redisTemplate.hasKey("auth:token:" + TokenHashUtil.sha256("old-token"))).isFalse();
    }
}
