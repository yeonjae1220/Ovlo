package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.GoogleLoginCommand;
import me.yeonjae.ovlo.application.dto.result.GoogleUserProfile;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.oauth.GoogleOAuthPort;
import me.yeonjae.ovlo.domain.auth.model.AuthSession;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GoogleAuthCommandServiceTest {

    @Mock
    private GoogleOAuthPort googleOAuthPort;
    @Mock
    private LoadMemberPort loadMemberPort;
    @Mock
    private SaveMemberPort saveMemberPort;
    @Mock
    private TokenStorePort tokenStorePort;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private GoogleAuthCommandService sut;

    @Test
    @DisplayName("Google 로그인 세션 만료는 설정된 refresh TTL 을 따른다(쿠키 maxAge 와 같은 소스)")
    void sessionExpiry_followsConfiguredRefreshTtl() {
        Duration refreshTtl = Duration.ofDays(45);
        MemberId memberId = new MemberId(3L);
        given(googleOAuthPort.getUserProfile("code", "https://ovlo.test/cb"))
                .willReturn(new GoogleUserProfile("g@example.com", "구글", null, "google-1"));
        given(loadMemberPort.findByEmail("g@example.com")).willReturn(Optional.of(googleMember(memberId)));
        given(jwtTokenProvider.generateAccessToken(memberId, MemberRole.MEMBER)).willReturn("access");
        given(jwtTokenProvider.generateRefreshToken()).willReturn("refresh");
        given(jwtTokenProvider.refreshTokenTtl()).willReturn(refreshTtl);

        Instant before = Instant.now();
        sut.loginWithGoogle(new GoogleLoginCommand("code", "https://ovlo.test/cb"));

        ArgumentCaptor<AuthSession> saved = ArgumentCaptor.forClass(AuthSession.class);
        then(tokenStorePort).should().save(saved.capture());
        assertThat(saved.getValue().getExpiresAt())
                .isBetween(before.plus(refreshTtl), Instant.now().plus(refreshTtl));
        then(saveMemberPort).should(never()).save(any());
    }

    private static Member googleMember(MemberId id) {
        return Member.restore(id, "구글", "구글", null,
                new Email("g@example.com"), null,
                OAuthProvider.GOOGLE, "google-1",
                null, null, MemberStatus.ACTIVE, null, null, null,
                List.of(), List.of(), List.of(),
                MemberRole.MEMBER);
    }
}
