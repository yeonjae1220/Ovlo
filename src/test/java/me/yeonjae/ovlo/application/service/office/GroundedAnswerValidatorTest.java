package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort.Candidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroundedAnswerValidatorTest {

    private final GroundedAnswerValidator validator = new GroundedAnswerValidator();
    private final List<Candidate> candidates = List.of(
            new Candidate(1, 77L, "기숙사 신청", "2월 10일까지", "https://univ.ac.kr/dorm"),
            new Candidate(2, 78L, "보험 가입", "입국 후 2주 안에", null));

    @Test
    @DisplayName("후보 안의 인용만 있으면 통과하고 인용 순서대로 id 를 돌려준다")
    void passesWithValidCitations() {
        var outcome = validator.validate("보험은 입국 후 2주 안에 가입합니다 [2]. 기숙사는 2월 10일까지입니다 [1].", candidates);

        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.validated().citedNoticeIds()).containsExactly(78L, 77L);
        assertThat(outcome.validated().citedIndices()).containsExactly(2, 1);
    }

    @Test
    @DisplayName("NO_GROUNDS·인용 없음·범위 밖 인용·빈 응답은 거부한다")
    void rejects() {
        assertThat(validator.validate("NO_GROUNDS", candidates).passed()).isFalse();
        assertThat(validator.validate("no_grounds — 자료에 없습니다", candidates).passed()).isFalse();
        assertThat(validator.validate("기숙사는 2월 10일까지입니다.", candidates).passed()).isFalse();
        assertThat(validator.validate("기숙사는 2월 10일까지입니다 [3].", candidates).rejection().reason()).contains("[3]");
        assertThat(validator.validate("  ", candidates).passed()).isFalse();
        assertThat(validator.validate(null, candidates).passed()).isFalse();
    }

    @Test
    @DisplayName("후보 sourceUrl 이 아닌 URL 은 지우고, 후보 URL 은 남긴다")
    void stripsForeignUrls() {
        var outcome = validator.validate(
                "신청은 https://univ.ac.kr/dorm 에서 합니다 [1]. 자세한 건 https://evil.example.com/x 참고 [1].", candidates);

        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.validated().text()).contains("https://univ.ac.kr/dorm").doesNotContain("evil.example.com");
    }
}
