package me.yeonjae.ovlo.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private static final String MEMBER_ID_CLAIM = "memberId";
    private static final String ISSUER = "ovlo";
    private static final String AUDIENCE = "ovlo-api";

    private final SecretKey secretKey;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlMinutes;

    public JwtTokenProviderImpl(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${jwt.refresh-token-ttl-minutes:10080}") long refreshTokenTtlMinutes) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlMinutes = refreshTokenTtlMinutes;
    }

    @Override
    public String generateAccessToken(MemberId memberId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenTtlMinutes * 60 * 1000L);

        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .claim(MEMBER_ID_CLAIM, memberId.value())
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
    public MemberId extractMemberId(String accessToken) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        Long memberId = claims.get(MEMBER_ID_CLAIM, Long.class);
        if (memberId == null) {
            throw new IllegalArgumentException("JWT 토큰에 memberId 클레임이 없습니다");
        }
        return new MemberId(memberId);
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(accessToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
