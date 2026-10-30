package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** JWT 의 담당자 소속 클레임(ADR-0005) 왕복을 고정한다. */
class JwtTokenProviderImplOfficeClaimsTest {

    // application-test.yml 과 같은 값(HS256 충분 길이)
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";

    private final JwtTokenProviderImpl provider = new JwtTokenProviderImpl(SECRET, 15, 43200);

    @Test
    @DisplayName("소속 없이 발급한 토큰에는 office 클레임이 없다")
    void noMembership() {
        String token = provider.generateAccessToken(new MemberId(42L), MemberRole.MEMBER);

        assertThat(provider.validateAccessToken(token)).isTrue();
        assertThat(provider.extractMemberId(token)).isEqualTo(new MemberId(42L));
        assertThat(provider.extractOfficeMembership(token)).isEmpty();
    }

    @Test
    @DisplayName("소속을 넣어 발급하면 officeId·officeRole 이 그대로 돌아온다")
    void roundTripsMembership() {
        var membership = new OfficeMembership(5L, OfficeRole.OFFICE_ADMIN);

        String token = provider.generateAccessToken(new MemberId(42L), MemberRole.MEMBER, membership);

        assertThat(provider.extractOfficeMembership(token)).contains(membership);
        assertThat(provider.extractRole(token)).isEqualTo(MemberRole.MEMBER);
    }

    @Test
    @DisplayName("officeId 가 없는(저장 전) 소속은 클레임으로 만들지 않는다")
    void unsavedOfficeIsNotClaimed() {
        String token = provider.generateAccessToken(new MemberId(42L), MemberRole.MEMBER,
                new OfficeMembership(null, OfficeRole.OFFICE_STAFF));

        assertThat(provider.extractOfficeMembership(token)).isEmpty();
    }
}
