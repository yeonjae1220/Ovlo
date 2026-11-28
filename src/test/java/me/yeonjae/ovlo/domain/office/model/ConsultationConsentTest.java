package me.yeonjae.ovlo.domain.office.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultationConsentTest {

    @Test
    @DisplayName("현재 동의 문구 v3 는 실제 보관 기준(상담을 연 날부터 1년)과 담당자에게 보이는 정보를 적는다")
    void currentVersionDescribesActualRetentionAndVisibility() {
        String text = ConsultationConsent.textFor(ConsultationConsent.CURRENT_VERSION);

        assertThat(ConsultationConsent.CURRENT_VERSION).isEqualTo("v3");
        assertThat(text).contains("상담을 연 날부터 1년").doesNotContain("학기 종료");
        assertThat(text).contains("닉네임").contains("프로필 사진");
    }

    @Test
    @DisplayName("v2 와 v3 동의는 대화 전체를 전달하고, v1 은 마지막 질문만 전달한다")
    void fullConversationVersions() {
        assertThat(ConsultationConsent.sharesFullConversation("v1")).isFalse();
        assertThat(ConsultationConsent.sharesFullConversation("v2")).isTrue();
        assertThat(ConsultationConsent.sharesFullConversation("v3")).isTrue();
        assertThat(ConsultationConsent.sharesFullConversation(null)).isFalse();
    }

    @Test
    @DisplayName("옛 동의 기록을 읽을 수 있도록 v1·v2 문구는 그대로 남는다")
    void oldVersionsRemainReadable() {
        assertThat(ConsultationConsent.textFor("v1")).contains("학기 종료 후 1년");
        assertThat(ConsultationConsent.textFor("v2")).contains("최대 20회");
    }
}
