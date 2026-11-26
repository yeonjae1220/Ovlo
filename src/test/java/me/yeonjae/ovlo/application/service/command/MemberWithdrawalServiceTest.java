package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AppleReauthorization;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.application.dto.result.AppleTokens;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleIdentityTokenPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort;
import me.yeonjae.ovlo.application.port.out.oauth.AppleTokenRevocationPort.TokenType;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberWithdrawalServiceTest {

    private static final long MEMBER_ID = 5L;
    private static final String SUB = "001234.apple.sub";
    private static final String WEB_CLIENT = "com.mungji.ovlo.web";
    private static final String RAW_NONCE = "raw-nonce-0123456789";
    private static final AppleReauthorization REAUTH =
            new AppleReauthorization("id-token", RAW_NONCE, "auth-code", "https://ovlo.test/login");

    @Mock private LoadMemberPort loadMemberPort;
    @Mock private MemberCommandService memberCommandService;
    @Mock private AppleIdentityTokenPort appleIdentityTokenPort;
    @Mock private AppleTokenRevocationPort appleTokenRevocationPort;

    @InjectMocks
    private MemberWithdrawalService sut;

    @Test
    @DisplayName("Apple 이 아닌 회원은 Apple 과 통신하지 않고 바로 탈퇴한다")
    void nonAppleMember_withdrawsDirectly() {
        given(loadMemberPort.findById(new MemberId(MEMBER_ID))).willReturn(Optional.of(member(OAuthProvider.GOOGLE, "g-1")));

        sut.withdraw(new WithdrawMemberCommand(MEMBER_ID));

        then(memberCommandService).should().withdraw(new WithdrawMemberCommand(MEMBER_ID));
        then(appleIdentityTokenPort).shouldHaveNoInteractions();
        then(appleTokenRevocationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Apple 회원은 Apple 재인증 결과로 토큰을 받아 폐기한 뒤에 탈퇴한다")
    void appleMember_revokesRefreshTokenThenWithdraws() {
        givenAppleMember();
        givenVerifiedIdentity(SUB, sha256Hex(RAW_NONCE));
        given(appleTokenRevocationPort.exchange("auth-code", WEB_CLIENT, "https://ovlo.test/login"))
                .willReturn(new AppleTokens(SUB, "refresh-token", "access-token"));

        sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH));

        InOrder order = inOrder(appleTokenRevocationPort, memberCommandService);
        order.verify(appleTokenRevocationPort).exchange("auth-code", WEB_CLIENT, "https://ovlo.test/login");
        order.verify(appleTokenRevocationPort).revoke(WEB_CLIENT, "refresh-token", TokenType.REFRESH_TOKEN);
        order.verify(memberCommandService).withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH));
    }

    @Test
    @DisplayName("교환 응답에 refresh token 이 없으면 access token 을 폐기한다")
    void noRefreshToken_revokesAccessToken() {
        givenAppleMember();
        givenVerifiedIdentity(SUB, sha256Hex(RAW_NONCE));
        given(appleTokenRevocationPort.exchange(anyString(), anyString(), any()))
                .willReturn(new AppleTokens(SUB, null, "access-token"));

        sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH));

        then(appleTokenRevocationPort).should().revoke(WEB_CLIENT, "access-token", TokenType.ACCESS_TOKEN);
        then(memberCommandService).should().withdraw(any());
    }

    @Test
    @DisplayName("Apple 회원이 재인증 없이 탈퇴를 요청하면 거절한다 — 토큰을 폐기할 방법이 없다")
    void appleMemberWithoutReauth_rejected() {
        givenAppleMember();

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.BAD_REQUEST);
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("다른 Apple ID 로 재인증하면 거절한다 — 이 회원의 연결은 그대로 남기 때문")
    void reauthWithAnotherAppleId_forbidden() {
        givenAppleMember();
        givenVerifiedIdentity("other-apple-sub", sha256Hex(RAW_NONCE));

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.FORBIDDEN);
        then(appleTokenRevocationPort).shouldHaveNoInteractions();
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("재인증 토큰의 nonce 가 맞지 않으면 거절한다")
    void nonceMismatch_rejected() {
        givenAppleMember();
        givenVerifiedIdentity(SUB, sha256Hex("another-nonce"));

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH)))
                .isInstanceOf(AuthException.class);
        then(appleTokenRevocationPort).shouldHaveNoInteractions();
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("authorization code 가 다른 Apple 사용자의 것이면 폐기하지도 탈퇴하지도 않는다")
    void codeOfAnotherUser_forbidden() {
        givenAppleMember();
        givenVerifiedIdentity(SUB, sha256Hex(RAW_NONCE));
        given(appleTokenRevocationPort.exchange(anyString(), anyString(), any()))
                .willReturn(new AppleTokens("someone-else", "refresh-token", "access-token"));

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorType())
                .isEqualTo(AuthException.ErrorType.FORBIDDEN);
        then(appleTokenRevocationPort).should(never()).revoke(anyString(), anyString(), any());
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("Apple 폐기가 실패하면 탈퇴하지 않는다 — 다시 시도하면 된다")
    void revocationFailure_keepsAccount() {
        givenAppleMember();
        givenVerifiedIdentity(SUB, sha256Hex(RAW_NONCE));
        given(appleTokenRevocationPort.exchange(anyString(), anyString(), any()))
                .willReturn(new AppleTokens(SUB, "refresh-token", null));
        org.mockito.BDDMockito.willThrow(new AuthException(AuthException.ErrorType.UNAVAILABLE, "down"))
                .given(appleTokenRevocationPort).revoke(anyString(), anyString(), any());

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH)))
                .isInstanceOf(AuthException.class);
        then(memberCommandService).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("없는 회원은 404")
    void unknownMember_notFound() {
        given(loadMemberPort.findById(new MemberId(MEMBER_ID))).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.withdraw(new WithdrawMemberCommand(MEMBER_ID, REAUTH)))
                .isInstanceOf(MemberException.class);
        then(memberCommandService).should(never()).withdraw(any());
    }

    private void givenAppleMember() {
        given(loadMemberPort.findById(new MemberId(MEMBER_ID))).willReturn(Optional.of(member(OAuthProvider.APPLE, SUB)));
    }

    private void givenVerifiedIdentity(String subject, String nonce) {
        given(appleIdentityTokenPort.verify("id-token"))
                .willReturn(new AppleIdentity(subject, "hidden@privaterelay.appleid.com", nonce, WEB_CLIENT));
    }

    private static Member member(OAuthProvider provider, String providerId) {
        return Member.restore(new MemberId(MEMBER_ID), "ovlo_member", "회원", null,
                new Email("hidden@privaterelay.appleid.com"), null, provider, providerId,
                null, null, MemberStatus.ACTIVE, null, null, null,
                List.of(), List.of(), List.of(), MemberRole.MEMBER);
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
