package me.yeonjae.ovlo.domain.moderation;

import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentReportTest {

    @Test
    @DisplayName("빈 상세 내용은 null 로 정규화한다")
    void blankDetailBecomesNull() {
        var report = new ContentReport(ReportTargetType.POST, 1, 2, 3, ReportReason.SPAM, "   ");
        assertThat(report.detail()).isNull();
    }

    @Test
    @DisplayName("기타 사유는 상세 내용이 필수다")
    void otherNeedsDetail() {
        assertThatThrownBy(() -> new ContentReport(ReportTargetType.POST, 1, 2, 3, ReportReason.OTHER, null))
                .isInstanceOf(ModerationException.class);
    }

    @Test
    @DisplayName("상세 내용은 1000자를 넘을 수 없다")
    void detailLimit() {
        String tooLong = "가".repeat(ContentReport.MAX_DETAIL_LENGTH + 1);
        assertThatThrownBy(() -> new ContentReport(ReportTargetType.POST, 1, 2, 3, ReportReason.SPAM, tooLong))
                .isInstanceOf(ModerationException.class);
    }

    @Test
    @DisplayName("신고자와 작성자가 같으면 거부한다")
    void selfReport() {
        assertThatThrownBy(() -> new ContentReport(ReportTargetType.MEMBER, 3, 3, 3, ReportReason.SPAM, null))
                .isInstanceOf(ModerationException.class);
    }
}
