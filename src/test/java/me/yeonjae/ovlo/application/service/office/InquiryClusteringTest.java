package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult.RepeatedQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryClusteringTest {

    @Test
    @DisplayName("대소문자·문장부호·공백 차이는 같은 질문으로 묶고, 최신 원문을 대표로 쓴다")
    void groupsNormalizedDuplicates() {
        List<RepeatedQuestion> out = InquiryClustering.cluster(List.of(
                "When does the dorm application close?",
                "기숙사 신청 언제까지예요?",
                "when does the dorm application close",
                "WHEN DOES THE DORM   APPLICATION CLOSE!!",
                "기숙사 신청 언제까지예요",
                "Can I work part-time?"), 10);

        assertThat(out).containsExactly(
                new RepeatedQuestion("When does the dorm application close?", 3),
                new RepeatedQuestion("기숙사 신청 언제까지예요?", 2));
    }

    @Test
    @DisplayName("1건뿐인 질문은 빼고, top 개수로 자르며, null·빈 문자열은 무시한다")
    void filtersAndLimits() {
        List<String> qs = Arrays.asList("a?", "a", "b", "b!", "c", "c", null, " ", "...");
        assertThat(InquiryClustering.cluster(qs, 2)).hasSize(2)
                .allSatisfy(r -> assertThat(r.count()).isEqualTo(2));
        assertThat(InquiryClustering.cluster(List.of("only once"), 10)).isEmpty();
    }
}
