package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.PartnerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartnerTest {

    private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    @DisplayName("등록 직후엔 꺼져 있고, 이름·URL 은 다듬어 저장된다")
    void createDisabledAndTrimmed() {
        Partner p = Partner.create(PartnerCategory.SIM, "  KT 유심 ", "  ", " https://sim.example ", 3, NOW);

        assertThat(p.isEnabled()).isFalse();
        assertThat(p.getName()).isEqualTo("KT 유심");
        assertThat(p.getDescription()).isNull();
        assertThat(p.getTargetUrl()).isEqualTo("https://sim.example");
        assertThat(p.getSortOrder()).isEqualTo(3);
        assertThat(p.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("이름이 비거나 링크가 http(s) 가 아니면 등록·수정할 수 없다")
    void rejectsInvalid() {
        assertThatThrownBy(() -> Partner.create(PartnerCategory.SIM, " ", null, "https://x", 0, NOW))
                .isInstanceOf(PartnerException.class)
                .extracting("errorType").isEqualTo(PartnerException.ErrorType.BAD_REQUEST);
        assertThatThrownBy(() -> Partner.create(PartnerCategory.SIM, "x", null, "javascript:alert(1)", 0, NOW))
                .isInstanceOf(PartnerException.class);
        assertThatThrownBy(() -> Partner.create(null, "x", null, "https://x", 0, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("켜고 끄고 수정할 수 있다")
    void toggleAndUpdate() {
        Partner p = Partner.create(PartnerCategory.SIM, "KT", null, "https://a", 0, NOW);
        p.enable();
        assertThat(p.isEnabled()).isTrue();
        p.update(PartnerCategory.INSURANCE, "DB손보", "유학생 보험", "https://b", 1);
        assertThat(p.getCategory()).isEqualTo(PartnerCategory.INSURANCE);
        assertThat(p.getDescription()).isEqualTo("유학생 보험");
        assertThat(p.isEnabled()).isTrue();
        p.disable();
        assertThat(p.isEnabled()).isFalse();
    }
}
