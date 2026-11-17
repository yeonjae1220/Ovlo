package me.yeonjae.ovlo.domain.office.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class InquiryRejectionReasonTest {
    @Test void reasonSurvivesEscalationAndRestoreWhileLegacyRowsRemainReadable() {
        Inquiry i = Inquiry.noGrounds(1L, 2L, NoticeLang.KO, "질문", List.of(), Instant.EPOCH, 0, null,
                InquiryRejectionReason.NO_CANDIDATES);
        i.markEscalated();
        assertThat(i.getRejectionReason()).isEqualTo(InquiryRejectionReason.NO_CANDIDATES);
        Inquiry restored = Inquiry.restore(3L, 1L, 2L, NoticeLang.KO, "질문", i.getOutcome(), null,
                List.of(), List.of(), Instant.EPOCH, 0, null, null, i.getRejectionReason());
        assertThat(restored.getRejectionReason()).isEqualTo(InquiryRejectionReason.NO_CANDIDATES);
        assertThat(Inquiry.restore(3L, 1L, 2L, NoticeLang.KO, "old", InquiryOutcome.NO_GROUNDS,
                null, List.of(), List.of(), Instant.EPOCH, 0, null).getRejectionReason()).isNull();
    }
}
