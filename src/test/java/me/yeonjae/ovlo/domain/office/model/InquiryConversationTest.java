package me.yeonjae.ovlo.domain.office.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class InquiryConversationTest {
    private Inquiry question(long office, long member) {
        return Inquiry.noGrounds(office, member, NoticeLang.KO, "기숙사 신청은?", List.of(), Instant.now(), 0, null);
    }

    @Test void followUpPreservesPreviousTurn() {
        Inquiry first = question(1, 2);
        first.assignId(10L);
        Inquiry next = question(1, 2).following(first);
        assertThat(next.getPreviousInquiryId()).isEqualTo(10L);
        assertThat(first.getPreviousInquiryId()).isNull();
    }

    @Test void cannotFollowAnotherStudentsOrSchoolsQuestion() {
        Inquiry first = question(1, 2);
        first.assignId(10L);
        assertThatThrownBy(() -> question(1, 3).following(first)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> question(2, 2).following(first)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void handoffEndsAiConversation() {
        Inquiry first = question(1, 2);
        first.assignId(10L);
        first.markEscalated();
        assertThatThrownBy(() -> question(1, 2).following(first)).hasMessageContaining("상담");
    }
}
