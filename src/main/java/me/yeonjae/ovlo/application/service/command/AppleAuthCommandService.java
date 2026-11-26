package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AppleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.application.dto.result.SocialLoginResult;
import me.yeonjae.ovlo.application.port.in.auth.AppleLoginUseCase;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleIdentityTokenPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

/**
 * Sign in with Apple.
 *
 * <p>회원은 Apple 사용자 식별자(sub)로 찾는다. 이메일은 사용자가 숨기면 relay 주소라 바뀔 수 있고,
 * 두 번째 로그인부터는 토큰에 없을 수도 있어 식별자로 쓰지 않는다. 같은 이메일이 다른 방법으로
 * 가입돼 있으면 계정을 합치지 않고 거절한다 — Apple 이 이메일 소유를 보증해도 이 서비스에서
 * 계정 연결은 본인이 로그인한 상태에서만 해야 하기 때문이다.
 */
@Service
@Transactional
public class AppleAuthCommandService implements AppleLoginUseCase {

    private static final String NICKNAME_PREFIX = "ovlo_";
    private static final String NICKNAME_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int NICKNAME_SUFFIX_LENGTH = 8;
    private static final int NICKNAME_ATTEMPTS = 10;
    private static final int NAME_MAX_LENGTH = 100;

    private final AppleIdentityTokenPort appleIdentityTokenPort;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final TokenStorePort tokenStorePort;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoadOfficeMembershipPort loadOfficeMembershipPort;
    private final SecureRandom random = new SecureRandom();

    public AppleAuthCommandService(AppleIdentityTokenPort appleIdentityTokenPort,
                                   LoadMemberPort loadMemberPort,
                                   SaveMemberPort saveMemberPort,
                                   TokenStorePort tokenStorePort,
                                   JwtTokenProvider jwtTokenProvider,
                                   LoadOfficeMembershipPort loadOfficeMembershipPort) {
        this.appleIdentityTokenPort = appleIdentityTokenPort;
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.tokenStorePort = tokenStorePort;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loadOfficeMembershipPort = loadOfficeMembershipPort;
    }

    @Override
    public SocialLoginResult loginWithApple(AppleLoginCommand command) {
        AppleIdentity identity = appleIdentityTokenPort.verify(command.identityToken());
        AppleNonce.requireMatching(command.rawNonce(), identity.nonce());

        Optional<Member> existing = loadMemberPort.findByProvider(OAuthProvider.APPLE, identity.subject());
        Member member;
        boolean newMember;
        if (existing.isPresent()) {
            member = existing.get();
            requireUsable(member);
            newMember = member.getStatus() == MemberStatus.PENDING_ONBOARDING;
        } else {
            member = saveMemberPort.save(createMember(identity, command.name()));
            newMember = true;
        }
        return issueSession(member, newMember);
    }

    private static void requireUsable(Member member) {
        if (member.getStatus() == MemberStatus.WITHDRAWN || member.getStatus() == MemberStatus.SUSPENDED) {
            throw new MemberException("사용할 수 없는 계정입니다", MemberException.ErrorType.FORBIDDEN);
        }
    }

    private Member createMember(AppleIdentity identity, String requestedName) {
        if (identity.email() == null || identity.email().isBlank()) {
            throw new AuthException("Apple 계정의 이메일을 받지 못했습니다. 이메일 공유를 허용해 다시 시도해 주세요");
        }
        if (loadMemberPort.findByEmail(identity.email()).isPresent()) {
            throw new MemberException(
                    "이미 다른 방법으로 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요",
                    MemberException.ErrorType.CONFLICT);
        }
        String nickname = uniqueNickname();
        String name = normalizeName(requestedName).orElse(nickname);
        return Member.createWithOAuth(nickname, name, new Email(identity.email()),
                OAuthProvider.APPLE, identity.subject());
    }

    private String uniqueNickname() {
        for (int attempt = 0; attempt < NICKNAME_ATTEMPTS; attempt++) {
            String candidate = NICKNAME_PREFIX + randomSuffix();
            if (!loadMemberPort.existsByNickname(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("사용 가능한 닉네임을 만들지 못했습니다");
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(NICKNAME_SUFFIX_LENGTH);
        for (int i = 0; i < NICKNAME_SUFFIX_LENGTH; i++) {
            suffix.append(NICKNAME_ALPHABET.charAt(random.nextInt(NICKNAME_ALPHABET.length())));
        }
        return suffix.toString();
    }

    private static Optional<String> normalizeName(String name) {
        if (name == null) return Optional.empty();
        String trimmed = name.strip();
        if (trimmed.isEmpty()) return Optional.empty();
        return Optional.of(trimmed.length() > NAME_MAX_LENGTH ? trimmed.substring(0, NAME_MAX_LENGTH) : trimmed);
    }

    // ADMIN 은 SSR 세션(/admin) 전용이라 JWT 에는 실리지 않는다(Google 로그인과 같은 규칙).
    private SocialLoginResult issueSession(Member member, boolean newMember) {
        MemberRole tokenRole = member.getRole() == MemberRole.UNIV ? MemberRole.UNIV : MemberRole.MEMBER;
        String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), tokenRole,
                loadOfficeMembershipPort.findActiveMembership(member.getId()).orElse(null));
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(jwtTokenProvider.refreshTokenTtl());
        tokenStorePort.save(AuthSession.create(member.getId(), refreshToken, expiresAt));
        return new SocialLoginResult(accessToken, refreshToken, member.getId().value(), newMember);
    }
}
