package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private final RateLimiterService rateLimiterService(int signupLimit) {
        return new RateLimiterService(redisTemplate, 10, 30, signupLimit, 100, 600);
    }

    private final RateLimiterService searchRateLimiter(int searchLimit) {
        return new RateLimiterService(redisTemplate, 10, 30, 5, searchLimit, 600);
    }

    @Test
    void checkSignupRate_belowLimit_doesNotThrow() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rl:signup:ip:203.0.113.9")), any()))
                .willReturn(1L);

        assertThatNoException().isThrownBy(() -> rateLimiterService(5).checkSignupRate("203.0.113.9"));
    }

    @Test
    void checkSignupRate_exceedsLimit_throwsTooManyRequests() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rl:signup:ip:203.0.113.9")), any()))
                .willReturn(6L);

        assertThatThrownBy(() -> rateLimiterService(5).checkSignupRate("203.0.113.9"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void checkSearchRate_belowLimit_doesNotThrow() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rl:search:ip:203.0.113.9")), any()))
                .willReturn(50L);

        assertThatNoException().isThrownBy(() -> searchRateLimiter(100).checkSearchRate("203.0.113.9"));
    }

    @Test
    void checkSearchRate_exceedsLimit_throwsTooManyRequests() {
        given(redisTemplate.execute(any(RedisScript.class), eq(List.of("rl:search:ip:203.0.113.9")), any()))
                .willReturn(101L);

        assertThatThrownBy(() -> searchRateLimiter(100).checkSearchRate("203.0.113.9"))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
