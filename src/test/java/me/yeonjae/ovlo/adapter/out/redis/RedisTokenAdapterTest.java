package me.yeonjae.ovlo.adapter.out.redis;

import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.auth.model.AuthSessionId;
import me.yeonjae.ovlo.domain.auth.model.RefreshRotationOutcome;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    // ── rotate: 제출 토큰 기준 compare-and-rotate ────────────────────────────

    private AuthSession saveBase(String token) {
        AuthSession base = AuthSession.create(memberId, token, expiresAt);
        adapter.save(base);
        return base;
    }

    private RefreshRotationOutcome rotate(AuthSessionId sessionId, String presented, String next) {
        AuthSession rotated = AuthSession.restore(sessionId, memberId, next,
                Instant.now().plus(7, ChronoUnit.DAYS), false);
        return adapter.rotate(rotated, presented);
    }

    private String sessionField(AuthSessionId sessionId, String field) {
        return (String) redisTemplate.opsForHash().get("auth:session:" + sessionId.value(), field);
    }

    @Test
    @DisplayName("현재 토큰을 제출하면 ROTATED — 새 토큰으로 조회되고 직전 토큰 해시가 기록된다")
    void shouldRotate_whenPresentedTokenIsCurrent() {
        AuthSession base = saveBase("old-token");

        RefreshRotationOutcome outcome = rotate(base.getId(), "old-token", "new-token");

        assertThat(outcome).isEqualTo(RefreshRotationOutcome.ROTATED);
        assertThat(adapter.findByRefreshToken("new-token")).isPresent();
        assertThat(sessionField(base.getId(), "refreshToken")).isEqualTo(TokenHashUtil.sha256("new-token"));
        assertThat(sessionField(base.getId(), "prevRefreshToken")).isEqualTo(TokenHashUtil.sha256("old-token"));
    }

    @Test
    @DisplayName("rotation 뒤에도 Redis 어디에도 평문 토큰이 남지 않는다 (GLOBAL-PIT-001)")
    void shouldNotStorePlaintext_afterRotation() {
        AuthSession base = saveBase("plain-old-token");
        rotate(base.getId(), "plain-old-token", "plain-new-token");

        Set<String> allKeys = redisTemplate.keys("*");
        assertThat(allKeys).noneMatch(k -> k.contains("plain-old-token") || k.contains("plain-new-token"));
        assertThat(redisTemplate.opsForHash().values("auth:session:" + base.getId().value()))
                .noneMatch(v -> v.equals("plain-old-token") || v.equals("plain-new-token"));
    }

    @Test
    @DisplayName("교체된 직전 토큰의 역인덱스는 재사용 감지 창만큼만 짧게 남는다")
    void shouldShortenPreviousTokenIndexTtl_onRotation() {
        AuthSession base = saveBase("old-token");

        rotate(base.getId(), "old-token", "new-token");

        Long prevTtl = redisTemplate.getExpire("auth:token:" + TokenHashUtil.sha256("old-token"));
        Long newTtl = redisTemplate.getExpire("auth:token:" + TokenHashUtil.sha256("new-token"));
        assertThat(prevTtl).isPositive().isLessThanOrEqualTo(RedisTokenAdapter.REUSE_DETECTION_WINDOW.toSeconds());
        assertThat(newTtl).isGreaterThan(prevTtl);
    }

    @Test
    @DisplayName("같은 이전 토큰으로 동시에 재발급하면 정확히 하나만 ROTATED, 나머지는 CONCURRENT")
    void shouldRotateExactlyOnce_underConcurrentRefreshWithSameToken() throws Exception {
        int contenders = 8;
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        try {
            for (int round = 0; round < 15; round++) {
                redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
                AuthSession base = saveBase("shared-old-" + round);
                String presented = "shared-old-" + round;

                CyclicBarrier barrier = new CyclicBarrier(contenders);
                List<Future<RefreshRotationOutcome>> futures = new ArrayList<>();
                for (int i = 0; i < contenders; i++) {
                    String next = "next-" + round + "-" + i;
                    futures.add(pool.submit(() -> {
                        barrier.await();
                        return rotate(base.getId(), presented, next);
                    }));
                }

                List<String> winners = new ArrayList<>();
                int concurrent = 0;
                for (int i = 0; i < contenders; i++) {
                    RefreshRotationOutcome outcome = futures.get(i).get();
                    if (outcome == RefreshRotationOutcome.ROTATED) winners.add("next-" + round + "-" + i);
                    if (outcome == RefreshRotationOutcome.CONCURRENT) concurrent++;
                }

                // single-use 계약: 같은 이전 토큰으로 시작한 재발급 중 정확히 하나만 성공한다
                assertThat(winners).as("라운드 %d ROTATED 수", round).hasSize(1);
                assertThat(concurrent).as("라운드 %d CONCURRENT 수", round).isEqualTo(contenders - 1);

                // 살아남은 토큰은 승자의 토큰이고, 패자가 만든 토큰은 어디에도 저장되지 않는다
                String winner = winners.get(0);
                assertThat(sessionField(base.getId(), "refreshToken")).isEqualTo(TokenHashUtil.sha256(winner));
                for (int i = 0; i < contenders; i++) {
                    String candidate = "next-" + round + "-" + i;
                    if (!candidate.equals(winner)) {
                        assertThat(adapter.findByRefreshToken(candidate)).as("패자 토큰 %s", candidate).isEmpty();
                    }
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("유예 시간이 지난 뒤 직전 토큰을 다시 제출하면 REUSED — 현재 토큰은 영향받지 않는다")
    void shouldDetectReuse_whenPreviousTokenPresentedAfterGrace() {
        AuthSession base = saveBase("old-token");
        rotate(base.getId(), "old-token", "new-token");
        // rotatedAt 을 유예 시간 밖으로 되돌린다
        long past = Instant.now().minus(RedisTokenAdapter.CONCURRENT_GRACE).minusSeconds(1).toEpochMilli();
        redisTemplate.opsForHash().put("auth:session:" + base.getId().value(), "rotatedAt", String.valueOf(past));

        RefreshRotationOutcome outcome = rotate(base.getId(), "old-token", "attacker-token");

        assertThat(outcome).isEqualTo(RefreshRotationOutcome.REUSED);
        assertThat(adapter.findByRefreshToken("attacker-token")).isEmpty();
        assertThat(sessionField(base.getId(), "refreshToken")).isEqualTo(TokenHashUtil.sha256("new-token"));
    }

    @Test
    @DisplayName("두 세대 이전 토큰을 제출하면 INVALID")
    void shouldReturnInvalid_whenOlderThanPreviousToken() {
        AuthSession base = saveBase("t0");
        rotate(base.getId(), "t0", "t1");
        rotate(base.getId(), "t1", "t2");

        assertThat(rotate(base.getId(), "t0", "t3")).isEqualTo(RefreshRotationOutcome.INVALID);
        assertThat(sessionField(base.getId(), "refreshToken")).isEqualTo(TokenHashUtil.sha256("t2"));
    }

    @Test
    @DisplayName("로그아웃과 경합한 재발급은 INVALID이고 세션을 되살리지 않는다")
    void shouldNotResurrectSession_whenRotateRacesLogout() {
        AuthSession base = saveBase("old-token");

        adapter.deleteByRefreshToken("old-token");
        RefreshRotationOutcome outcome = rotate(base.getId(), "old-token", "new-token");

        assertThat(outcome).isEqualTo(RefreshRotationOutcome.INVALID);
        assertThat(redisTemplate.hasKey("auth:session:" + base.getId().value())).isFalse();
        assertThat(adapter.findByRefreshToken("new-token")).isEmpty();
    }

    @Test
    @DisplayName("무효화된 세션은 현재 토큰이라도 INVALID")
    void shouldReturnInvalid_whenSessionRevoked() {
        AuthSession base = saveBase("old-token");
        redisTemplate.opsForHash().put("auth:session:" + base.getId().value(), "revoked", "true");

        assertThat(rotate(base.getId(), "old-token", "new-token")).isEqualTo(RefreshRotationOutcome.INVALID);
    }
}
