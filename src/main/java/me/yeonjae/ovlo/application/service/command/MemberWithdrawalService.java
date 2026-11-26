package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AppleReauthorization;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.application.dto.result.AppleTokens;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleIdentityTokenPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort.TokenType;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import org.springframework.stereotype.Service;

/**
 * 회원 탈퇴 진입점.
 *
 * <p>Apple 로그인 회원은 탈퇴 전에 Apple 쪽 연결(토큰)을 폐기해야 한다(App Store 5.1.1(v)). 토큰을 보관하지
 * 않는 대신 탈퇴 직전에 Apple 로 다시 인증받아 얻은 authorization code 를 교환해 곧바로 폐기한다.
 * Apple 과의 통신은 DB 트랜잭션 밖에서 먼저 하고, 성공한 뒤에만 {@link MemberCommandService#withdraw}
 * (트랜잭션)를 부른다. 폐기가 실패하면 탈퇴하지 않는다 — 사용자가 다시 시도하면 된다. 폐기 뒤 탈퇴가
 * 실패하면 Apple 연결만 끊긴 채 계정이 남는데, 다음 Apple 로그인이 연결을 다시 만들 뿐이라 해가 없다.
 *
 * <p>Apple 재인증을 아예 할 수 없는 회원(Apple ID 분실, Apple 장애·키 미설정으로 폐기가 계속 실패)은
 * {@link AdminMemberWithdrawalService} 로 관리자가 대신 탈퇴시킨다. 이때 Apple 토큰은 폐기하지 못하므로, 사용자에게
 * 설정 &gt; Apple ID &gt; 'Apple로 로그인' 에서 Ovlo 를 직접 지우도록 안내한다.
 */
@Service
public class MemberWithdrawalService implements WithdrawMemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final MemberCommandService memberCommandService;
    private final AppleIdentityTokenPort appleIdentityTokenPort;
    private final AppleTokenRevocationPort appleTokenRevocationPort;

    public MemberWithdrawalService(LoadMemberPort loadMemberPort,
                                   MemberCommandService memberCommandService,
                                   AppleIdentityTokenPort appleIdentityTokenPort,
                                   AppleTokenRevocationPort appleTokenRevocationPort) {
        this.loadMemberPort = loadMemberPort;
        this.memberCommandService = memberCommandService;
        this.appleIdentityTokenPort = appleIdentityTokenPort;
        this.appleTokenRevocationPort = appleTokenRevocationPort;
    }

    @Override
    public void withdraw(WithdrawMemberCommand command) {
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(),
                        MemberException.ErrorType.NOT_FOUND));
        if (member.getProvider() == OAuthProvider.APPLE) {
            revokeAppleTokens(member, command.apple());
        }
        memberCommandService.withdraw(command);
    }

    private void revokeAppleTokens(Member member, AppleReauthorization reauth) {
        if (reauth == null) {
            throw new AuthException(AuthException.ErrorType.BAD_REQUEST,
                    "Apple 로그인 계정은 탈퇴 전에 Apple 인증이 한 번 더 필요합니다");
        }
        AppleIdentity identity = appleIdentityTokenPort.verify(reauth.identityToken());
        AppleNonce.requireMatching(reauth.rawNonce(), identity.nonce());
        requireSameAppleId(member, identity.subject());

        AppleTokens tokens = appleTokenRevocationPort.exchange(
                reauth.authorizationCode(), identity.audience(), reauth.redirectUri());
        requireSameAppleId(member, tokens.subject());

        if (tokens.refreshToken() != null) {
            appleTokenRevocationPort.revoke(identity.audience(), tokens.refreshToken(), TokenType.REFRESH_TOKEN);
        } else if (tokens.accessToken() != null) {
            appleTokenRevocationPort.revoke(identity.audience(), tokens.accessToken(), TokenType.ACCESS_TOKEN);
        } else {
            throw new AuthException(AuthException.ErrorType.UNAVAILABLE,
                    "Apple 계정 연결을 해제하지 못했습니다. 잠시 후 다시 시도해 주세요");
        }
    }

    private static void requireSameAppleId(Member member, String subject) {
        if (!member.getProviderId().equals(subject)) {
            throw new AuthException(AuthException.ErrorType.FORBIDDEN,
                    "이 계정에 연결된 Apple ID 로 인증해 주세요");
        }
    }
}
