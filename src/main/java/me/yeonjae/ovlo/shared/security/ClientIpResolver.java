package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 신뢰 프록시(nginx-ingress 등) IP/CIDR 대역에서 온 요청만 X-Real-IP 헤더를 신뢰해
 * 실제 클라이언트 IP를 추출한다.
 * 그렇지 않으면 클라이언트가 헤더를 위조해 IP 기반 rate limit을 우회할 수 있다.
 */
@Component
public class ClientIpResolver {

    private final List<IpAddressMatcher> trustedProxyMatchers;

    public ClientIpResolver(@Value("${ovlo.trusted-proxy-ips:127.0.0.1,::1}") String trustedProxyIpsConfig) {
        this.trustedProxyMatchers = Arrays.stream(trustedProxyIpsConfig.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .map(this::parseMatcher)
                .toList();
    }

    private IpAddressMatcher parseMatcher(String ip) {
        try {
            return new IpAddressMatcher(ip);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ovlo.trusted-proxy-ips에 잘못된 IP/CIDR 항목이 있습니다: " + ip, e);
        }
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (isTrustedProxy(remoteAddr)) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddr));
    }
}
