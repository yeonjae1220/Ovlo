package me.yeonjae.ovlo.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver("10.0.0.1,10.42.0.0/16");

    @Test
    @DisplayName("신뢰되지 않은 피어의 X-Real-IP는 무시하고 실제 TCP 피어 주소를 반환한다")
    void resolve_untrustedPeer_ignoresRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Real-IP", "1.1.1.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("신뢰 프록시(allowlist 등록 IP)에서 온 요청은 X-Real-IP의 실제 클라이언트 IP를 사용한다")
    void resolve_trustedProxyPeer_usesRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("신뢰 프록시 CIDR 대역에 속한 피어에서 온 요청도 X-Real-IP를 신뢰한다")
    void resolve_trustedProxyCidrRange_usesRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.42.3.7");
        request.addHeader("X-Real-IP", "198.51.100.8");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.8");
    }

    @Test
    @DisplayName("공격자가 매 요청 다른 X-Real-IP 값을 보내도 신뢰되지 않은 피어라면 동일한 실제 IP로 식별된다 (스푸핑 우회 차단)")
    void resolve_spoofedRealIpVariesPerRequest_stillReturnsRealPeerIp() {
        MockHttpServletRequest first = new MockHttpServletRequest();
        first.setRemoteAddr("203.0.113.9");
        first.addHeader("X-Real-IP", "9.9.9.1");

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.setRemoteAddr("203.0.113.9");
        second.addHeader("X-Real-IP", "9.9.9.2");

        assertThat(resolver.resolve(first)).isEqualTo(resolver.resolve(second)).isEqualTo("203.0.113.9");
    }
}
