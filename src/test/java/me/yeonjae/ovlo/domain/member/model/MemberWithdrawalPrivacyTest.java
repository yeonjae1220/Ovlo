package me.yeonjae.ovlo.domain.member.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MemberWithdrawalPrivacyTest {
    @Test void withdrawalErasesCredentialsAndNeverRestoresOldIdentity() {
        Member member = Member.createWithOAuth("before", "Private Name", new Email("private@example.invalid"), OAuthProvider.GOOGLE, "private-provider-id");
        member.withdraw();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getEmail().value()).endsWith("@deleted.invalid").doesNotContain("private");
        assertThat(member.getName()).isEqualTo("Withdrawn member");
        assertThat(member.getProviderId()).isNull();
        assertThat(member.getPassword()).isNull();
        assertThat(member.getProfileImageMediaId()).isNull();
        assertThat(member.getLanguageSkills()).isEmpty();
        assertThatThrownBy(member::reactivateForOnboarding).isInstanceOf(IllegalStateException.class);
    }
}
