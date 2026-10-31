package me.yeonjae.ovlo.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bearer JWT → SecurityContext. principal 은 memberId(Long).
 * 권한: 플랫폼 ADMIN 이면 {@code ADMIN}, 아니면 {@code ROLE_USER}. 담당자 클레임이 있으면
 * {@link #OFFICE_STAFF_AUTHORITY}(관리자면 {@link #OFFICE_ADMIN_AUTHORITY} 도) 를 더하고,
 * 소속 자체는 {@code details} 에 {@link OfficeMembership} 으로 실어 어댑터가 범위 검사에 쓴다(ADR-0005).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String OFFICE_STAFF_AUTHORITY = "OFFICE_STAFF";
    public static final String OFFICE_ADMIN_AUTHORITY = "OFFICE_ADMIN";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
            MemberId memberId = jwtTokenProvider.extractMemberId(token);
            MemberRole role = jwtTokenProvider.extractRole(token);
            Optional<OfficeMembership> membership = jwtTokenProvider.extractOfficeMembership(token);

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role == MemberRole.ADMIN ? "ADMIN" : "ROLE_USER"));
            membership.ifPresent(m -> {
                authorities.add(new SimpleGrantedAuthority(OFFICE_STAFF_AUTHORITY));
                if (m.isAdmin()) authorities.add(new SimpleGrantedAuthority(OFFICE_ADMIN_AUTHORITY));
            });

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    memberId.value(), null, authorities);
            membership.ifPresent(authentication::setDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MDC.put("userId", memberId.value().toString());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
