package me.yeonjae.ovlo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ROLE_CLAIM = "role";
    private static final String OFFICE_ID_CLAIM = "officeId";
    private static final String OFFICE_ROLE_CLAIM = "officeRole";
    private static final String ISSUER = "ovlo";
    private static final String AUDIENCE = "ovlo-api";

    private final SecretKey secretKey;
    private final long accessTokenTtlMinutes;
    private final Duration refreshTokenTtl;

    public JwtTokenProviderImpl(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${jwt.refresh-token-ttl-minutes:43200}") long refreshTokenTtlMinutes) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        if (refreshTokenTtlMinutes <= 0) {
            throw new IllegalArgumentException("jwt.refresh-token-ttl-minutes 는 0보다 커야 합니다: " + refreshTokenTtlMinutes);
        }
        this.refreshTokenTtl = Duration.ofMinutes(refreshTokenTtlMinutes);
    }

    @Override
    public String generateAccessToken(MemberId memberId, MemberRole role, OfficeMembership officeMembership) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenTtlMinutes * 60 * 1000L);

        JwtBuilder builder = Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim(MEMBER_ID_CLAIM, memberId.value())
                .claim(ROLE_CLAIM, role != null ? role.name() : MemberRole.MEMBER.name());
        // 저장되지 않은 Office(officeId null) 소속은 클레임으로 만들지 않는다 — 범위 검사가 어차피 거부한다.
        if (officeMembership != null && officeMembership.officeId() != null) {
            builder.claim(OFFICE_ID_CLAIM, officeMembership.officeId())
                   .claim(OFFICE_ROLE_CLAIM, officeMembership.role().name());
        }
        return builder
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    @Override
    public MemberId extractMemberId(String accessToken) {
        Long memberId = parse(accessToken).get(MEMBER_ID_CLAIM, Long.class);
        if (memberId == null) {
            throw new IllegalArgumentException("JWT 토큰에 memberId 클레임이 없습니다");
        }
        return new MemberId(memberId);
    }

    @Override
    public MemberRole extractRole(String accessToken) {
        String role = parse(accessToken).get(ROLE_CLAIM, String.class);
        if (role == null) {
            return MemberRole.MEMBER;
        }
        try {
            return MemberRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return MemberRole.MEMBER;
        }
    }

    @Override
    public Optional<OfficeMembership> extractOfficeMembership(String accessToken) {
        Claims claims = parse(accessToken);
        Long officeId = claims.get(OFFICE_ID_CLAIM, Long.class);
        String officeRole = claims.get(OFFICE_ROLE_CLAIM, String.class);
        if (officeId == null || officeRole == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new OfficeMembership(officeId, OfficeRole.valueOf(officeRole)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            parse(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String accessToken) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }
}
