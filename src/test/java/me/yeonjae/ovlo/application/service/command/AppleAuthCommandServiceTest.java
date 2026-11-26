package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AppleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.AppleIdentity;
import me.yeonjae.ovlo.application.dto.result.SocialLoginResult;
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
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppleAuthCommandServiceTest {

    private static final String TOKEN = "apple-identity-token";
    private static final String RAW_NONCE = "raw-nonce-0123456789";
    private static final String SUB = "001234.abcdef.0123";

    @Mock private AppleIdentityTokenPort appleIdentityTokenPort;
    @Mock private LoadMemberPort loadMemberPort;
    @Mock private SaveMemberPort saveMemberPort;
    @Mock private TokenStorePort tokenStorePort;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private LoadOfficeMembershipPort loadOfficeMembershipPort;

    @InjectMocks
    private AppleAuthCommandService sut;

    @BeforeEach
    void tokens() {
        given(jwtTokenProvider.generateAccessToken(any(), any(), any())).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken()).willReturn("refresh");
        given(jwtTokenProvider.refreshTokenTtl()).willReturn(Duration.ofDays(30));
        given(loadOfficeMembershipPort.findActiveMembership(any())).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("처음 로그인한 Apple 사용자는 PENDING_ONBOARDING 회원으로 만들어지고 세션이 발급된다")
    void firstLogin_createsPendingMember() {
        givenVerifiedToken("hidden@privaterelay.appleid.com");
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.empty());
        given(loadMemberPort.findByEmail("hidden@privaterelay.appleid.com")).willReturn(Optional.empty());
        given(loadMemberPort.existsByNickname(anyString())).willReturn(false);
        given(saveMemberPort.save(any())).willAnswer(inv -> withId(inv.getArgument(0), 11L));

        SocialLoginResult result = sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, "  홍길동  "));

        ArgumentCaptor<Member> created = ArgumentCaptor.forClass(Member.class);
        then(saveMemberPort).should().save(created.capture());
        Member member = created.getValue();
        assertThat(member.getProvider()).isEqualTo(OAuthProvider.APPLE);
        assertThat(member.getProviderId()).isEqualTo(SUB);
        assertThat(member.getEmail().value()).isEqualTo("hidden@privaterelay.appleid.com");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getNickname()).startsWith("ovlo_");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING_ONBOARDING);
        assertThat(result.newMember()).isTrue();
        assertThat(result.memberId()).isEqualTo(11L);
        assertThat(result.accessToken()).isEqualTo("access");
        then(tokenStorePort).should().save(any(AuthSession.class));
    }

    @Test
    @DisplayName("Apple 이 이름을 주지 않으면(두 번째 기기 등) 생성된 닉네임을 이름으로 쓴다")
    void firstLogin_withoutName_usesNickname() {
        givenVerifiedToken("a@example.com");
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.empty());
        given(loadMemberPort.findByEmail("a@example.com")).willReturn(Optional.empty());
        given(loadMemberPort.existsByNickname(anyString())).willReturn(false);
        given(saveMemberPort.save(any())).willAnswer(inv -> withId(inv.getArgument(0), 12L));

        sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null));

        ArgumentCaptor<Member> created = ArgumentCaptor.forClass(Member.class);
        then(saveMemberPort).should().save(created.capture());
        assertThat(created.getValue().getName()).isEqualTo(created.getValue().getNickname());
    }

    @Test
    @DisplayName("이미 쓰인 닉네임이 나오면 다른 닉네임을 다시 만든다")
    void firstLogin_retriesTakenNickname() {
        givenVerifiedToken("a@example.com");
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.empty());
        given(loadMemberPort.findByEmail("a@example.com")).willReturn(Optional.empty());
        given(loadMemberPort.existsByNickname(anyString())).willReturn(true, true, false);
        given(saveMemberPort.save(any())).willAnswer(inv -> withId(inv.getArgument(0), 13L));

        sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null));

        then(loadMemberPort).should(org.mockito.Mockito.times(3)).existsByNickname(anyString());
    }

    @Test
    @DisplayName("Apple 사용자 식별자(sub)로 기존 회원을 찾는다 — 이메일을 숨긴 relay 주소가 바뀌어도 같은 회원")
    void returningMember_matchedBySubject() {
        givenVerifiedToken(null);
        Member existing = appleMember(new MemberId(20L), MemberStatus.ACTIVE);
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.of(existing));

        SocialLoginResult result = sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, "새이름"));

        assertThat(result.memberId()).isEqualTo(20L);
        assertThat(result.newMember()).isFalse();
        then(saveMemberPort).should(never()).save(any());
        then(loadMemberPort).should(never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("온보딩을 마치지 않은 기존 Apple 회원은 newMember=true 로 온보딩에 다시 보낸다")
    void returningPendingMember_isStillNew() {
        givenVerifiedToken(null);
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB))
                .willReturn(Optional.of(appleMember(new MemberId(21L), MemberStatus.PENDING_ONBOARDING)));

        assertThat(sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)).newMember()).isTrue();
    }

    @Test
    @DisplayName("정지·탈퇴 회원은 Apple 로그인으로도 세션을 받지 못한다")
    void suspendedOrWithdrawn_forbidden() {
        for (MemberStatus status : List.of(MemberStatus.SUSPENDED, MemberStatus.WITHDRAWN)) {
            givenVerifiedToken(null);
            given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB))
                    .willReturn(Optional.of(appleMember(new MemberId(22L), status)));

            assertThatThrownBy(() -> sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)))
                    .isInstanceOf(MemberException.class);
        }
        then(tokenStorePort).should(never()).save(any());
    }

    @Test
    @DisplayName("같은 이메일이 다른 방법(이메일·Google)으로 가입돼 있으면 계정을 합치지 않고 거절한다")
    void emailOwnedByAnotherProvider_conflict() {
        givenVerifiedToken("taken@example.com");
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.empty());
        Member google = Member.createWithOAuth("g", "g", new Email("taken@example.com"), OAuthProvider.GOOGLE, "google-1");
        given(loadMemberPort.findByEmail("taken@example.com")).willReturn(Optional.of(google));

        assertThatThrownBy(() -> sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)))
                .isInstanceOf(MemberException.class)
                .extracting(e -> ((MemberException) e).getErrorType())
                .isEqualTo(MemberException.ErrorType.CONFLICT);
        then(saveMemberPort).should(never()).save(any());
        then(tokenStorePort).should(never()).save(any());
    }

    @Test
    @DisplayName("처음 로그인인데 토큰에 이메일이 없으면 가입시키지 않는다")
    void firstLogin_withoutEmail_rejected() {
        givenVerifiedToken(null);
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)))
                .isInstanceOf(AuthException.class);
        then(saveMemberPort).should(never()).save(any());
    }

    @Test
    @DisplayName("토큰의 nonce 가 보낸 nonce 의 SHA-256 과 다르면 재사용된 토큰으로 보고 거절한다")
    void nonceMismatch_rejected() {
        given(appleIdentityTokenPort.verify(TOKEN))
                .willReturn(new AppleIdentity(SUB, "a@example.com", sha256Hex("other-nonce"), "com.mungji.ovlo"));

        assertThatThrownBy(() -> sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)))
                .isInstanceOf(AuthException.class);
        then(loadMemberPort).should(never()).findByProvider(any(), anyString());
        then(tokenStorePort).should(never()).save(any());
    }

    @Test
    @DisplayName("토큰에 nonce 가 아예 없으면 거절한다")
    void missingNonceClaim_rejected() {
        given(appleIdentityTokenPort.verify(TOKEN)).willReturn(new AppleIdentity(SUB, "a@example.com", null, "com.mungji.ovlo"));

        assertThatThrownBy(() -> sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null)))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Apple 로그인 회원에게도 ADMIN 권한은 JWT 로 나가지 않는다")
    void adminRoleIsNotIssuedInJwt() {
        givenVerifiedToken(null);
        Member admin = appleMember(new MemberId(23L), MemberStatus.ACTIVE);
        admin.updateRole(MemberRole.ADMIN);
        given(loadMemberPort.findByProvider(OAuthProvider.APPLE, SUB)).willReturn(Optional.of(admin));

        sut.loginWithApple(new AppleLoginCommand(TOKEN, RAW_NONCE, null));

        then(jwtTokenProvider).should().generateAccessToken(new MemberId(23L), MemberRole.MEMBER, null);
    }

    private void givenVerifiedToken(String email) {
        given(appleIdentityTokenPort.verify(TOKEN)).willReturn(new AppleIdentity(SUB, email, sha256Hex(RAW_NONCE), "com.mungji.ovlo"));
    }

    private static Member appleMember(MemberId id, MemberStatus status) {
        return Member.restore(id, "ovlo_apple1", "애플", null,
                new Email("hidden@privaterelay.appleid.com"), null,
                OAuthProvider.APPLE, SUB,
                null, null, status, null, null, null,
                List.of(), List.of(), List.of(),
                MemberRole.MEMBER);
    }

    private static Member withId(Member m, long id) {
        return Member.restore(new MemberId(id), m.getNickname(), m.getName(), null, m.getEmail(), null,
                m.getProvider(), m.getProviderId(), null, null, m.getStatus(), null, null, null,
                List.of(), List.of(), List.of(), m.getRole());
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
