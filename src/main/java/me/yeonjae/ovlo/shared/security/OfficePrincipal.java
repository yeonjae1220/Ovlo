package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/** 현재 인증에서 담당자 소속 클레임을 꺼낸다. {@link JwtAuthenticationFilter} 가 details 에 실은 값. */
public final class OfficePrincipal {

    private OfficePrincipal() {}

    public static Optional<OfficeMembership> membershipOf(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        Object details = authentication.getDetails();
        return details instanceof OfficeMembership m ? Optional.of(m) : Optional.empty();
    }

    /** 범위 검사 정책에 넘길 값. 클레임이 없으면 null (정책이 FORBIDDEN 으로 처리). */
    public static OfficeMembership membershipOrNull(Authentication authentication) {
        return membershipOf(authentication).orElse(null);
    }
}
