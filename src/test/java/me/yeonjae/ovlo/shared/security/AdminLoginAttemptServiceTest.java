package me.yeonjae.ovlo.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminLoginAttemptServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private AdminLoginAttemptService service() {
        // maxAttempts=5, failWindow=300s, lockout=900s — lab-dashboard 임계값과 동일
        return new AdminLoginAttemptService(redisTemplate, 5, 300, 900);
    }

    @Test
    @DisplayName("recordFailure는 IP와 (정규화된)계정 두 차원 모두에 대해 카운터 스크립트를 실행한다")
    void recordFailure_incrementsBothIpAndAccountDimensions() {
        service().recordFailure("203.0.113.9", "Admin@Example.com");

        verify(redisTemplate).execute(any(RedisScript.class),
                eq(List.of("admin:login:fail:ip:203.0.113.9", "admin:login:lock:ip:203.0.113.9")),
                eq("300"), eq("5"), eq("900"));
        // username은 소문자/trim 정규화되어 계정 차원 카운터에 사용
        verify(redisTemplate).execute(any(RedisScript.class),
                eq(List.of("admin:login:fail:acc:admin@example.com", "admin:login:lock:acc:admin@example.com")),
                eq("300"), eq("5"), eq("900"));
    }

    @Test
    @DisplayName("username이 비어 있으면 IP 차원만 카운트한다 (계정 차원 생략)")
    void recordFailure_blankUsername_onlyIpDimension() {
        service().recordFailure("203.0.113.9", "  ");

        verify(redisTemplate, times(1)).execute(any(RedisScript.class), any(List.class),
                any(), any(), any());
        verify(redisTemplate).execute(any(RedisScript.class),
                eq(List.of("admin:login:fail:ip:203.0.113.9", "admin:login:lock:ip:203.0.113.9")),
                any(), any(), any());
    }

    @Test
    @DisplayName("락이 없으면 unlocked를 반환한다")
    void checkLock_noLock_returnsUnlocked() {
        given(redisTemplate.getExpire("admin:login:lock:ip:203.0.113.9", TimeUnit.SECONDS)).willReturn(-2L);
        given(redisTemplate.getExpire("admin:login:lock:acc:admin@example.com", TimeUnit.SECONDS)).willReturn(-2L);

        AdminLoginAttemptService.LockStatus status = service().checkLock("203.0.113.9", "admin@example.com");

        assertThat(status.locked()).isFalse();
    }

    @Test
    @DisplayName("IP 락이 걸려 있으면 남은 TTL을 retryAfter로 담아 locked를 반환한다")
    void checkLock_ipLocked_returnsLockedWithRetryAfter() {
        given(redisTemplate.getExpire("admin:login:lock:ip:203.0.113.9", TimeUnit.SECONDS)).willReturn(840L);
        given(redisTemplate.getExpire("admin:login:lock:acc:admin@example.com", TimeUnit.SECONDS)).willReturn(-2L);

        AdminLoginAttemptService.LockStatus status = service().checkLock("203.0.113.9", "admin@example.com");

        assertThat(status.locked()).isTrue();
        assertThat(status.retryAfterSeconds()).isEqualTo(840L);
    }

    @Test
    @DisplayName("계정 락만 걸려 있어도 locked를 반환한다 (분산 IP 무차별 대입 차단)")
    void checkLock_accountLocked_returnsLocked() {
        given(redisTemplate.getExpire("admin:login:lock:ip:203.0.113.9", TimeUnit.SECONDS)).willReturn(-2L);
        given(redisTemplate.getExpire("admin:login:lock:acc:admin@example.com", TimeUnit.SECONDS)).willReturn(600L);

        AdminLoginAttemptService.LockStatus status = service().checkLock("203.0.113.9", "admin@example.com");

        assertThat(status.locked()).isTrue();
        assertThat(status.retryAfterSeconds()).isEqualTo(600L);
    }

    @Test
    @DisplayName("username이 비어 있으면 계정 락은 조회하지 않고 IP 락만 본다")
    void checkLock_blankUsername_onlyChecksIp() {
        given(redisTemplate.getExpire("admin:login:lock:ip:203.0.113.9", TimeUnit.SECONDS)).willReturn(-2L);

        AdminLoginAttemptService.LockStatus status = service().checkLock("203.0.113.9", null);

        assertThat(status.locked()).isFalse();
        verify(redisTemplate, never()).getExpire(eq("admin:login:lock:acc:null"), any());
    }

    @Test
    @DisplayName("recordSuccess는 IP·계정의 실패 카운터와 락 키를 모두 삭제한다")
    void recordSuccess_clearsAllCounters() {
        service().recordSuccess("203.0.113.9", "admin@example.com");

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                "admin:login:fail:ip:203.0.113.9",
                "admin:login:lock:ip:203.0.113.9",
                "admin:login:fail:acc:admin@example.com",
                "admin:login:lock:acc:admin@example.com");
    }
}
