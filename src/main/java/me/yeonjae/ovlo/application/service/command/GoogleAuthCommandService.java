package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.GoogleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.GoogleLoginResult;
import me.yeonjae.ovlo.application.dto.result.GoogleUserProfile;
import me.yeonjae.ovlo.application.port.in.auth.GoogleLoginUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.oauth.GoogleOAuthPort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Transactional
public class GoogleAuthCommandService implements GoogleLoginUseCase {

    private static final long REFRESH_TOKEN_TTL_DAYS = 7L;

    private final GoogleOAuthPort googleOAuthPort;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final TokenStorePort tokenStorePort;
    private final JwtTokenProvider jwtTokenProvider;

    public GoogleAuthCommandService(GoogleOAuthPort googleOAuthPort,
                                    LoadMemberPort loadMemberPort,
                                    SaveMemberPort saveMemberPort,
                                    TokenStorePort tokenStorePort,
                                    JwtTokenProvider jwtTokenProvider) {
        this.googleOAuthPort = googleOAuthPort;
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.tokenStorePort = tokenStorePort;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public GoogleLoginResult loginWithGoogle(GoogleLoginCommand command) {
        GoogleUserProfile profile = googleOAuthPort.getUserProfile(command.code(), command.redirectUri());

        Optional<Member> existingMember = loadMemberPort.findByEmail(profile.email());

        boolean isNewMember;
        Member member;

        if (existingMember.isPresent()) {
            member = existingMember.get();
            if (member.getProvider() == OAuthProvider.LOCAL) {
                throw new MemberException(
                        "이미 이메일/비밀번호로 가입된 계정입니다. 일반 로그인을 사용해 주세요",
                        MemberException.ErrorType.CONFLICT);
            }
            if (member.getStatus() == MemberStatus.WITHDRAWN) {
                member.reactivateForOnboarding();
                member = saveMemberPort.save(member);
                isNewMember = true;
            } else {
                isNewMember = false;
            }
        } else {
            member = Member.createWithOAuth(
                    profile.name(),
                    profile.name(),
                    new Email(profile.email()),
                    OAuthProvider.GOOGLE,
                    profile.googleId());
            member = saveMemberPort.save(member);
            isNewMember = true;
        }

        String accessToken = jwtTokenProvider.generateAccessToken(member.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS);

        AuthSession session = AuthSession.create(member.getId(), refreshToken, expiresAt);
        tokenStorePort.save(session);

        return new GoogleLoginResult(accessToken, refreshToken, member.getId().value(), isNewMember);
    }
}
