package me.yeonjae.ovlo.adapter.out.office;

import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KeywordNoticeSearchAdapterTest {

    private static final Instant NOW = Instant.parse("2027-03-02T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 2);

    @Mock LoadNoticePort loadNoticePort;

    private static Notice notice(Long id, String title, String body) {
        Notice n = Notice.register(5L, NoticeCategory.OTHER, title, body, null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 30), 11L, NOW);
        n.assignId(id);
        n.approve("11", NOW, LocalDate.of(2027, 2, 1));
        return n;
    }

    @Test
    @DisplayName("한국어 질문은 글자 2개 묶음으로, 영어 질문은 단어로 겹침을 세어 많이 겹치는 순으로 돌려준다")
    void ranksByOverlap() {
        Notice dorm = notice(1L, "기숙사 신청 안내", "기숙사 신청은 2월 10일까지");
        Notice visa = notice(2L, "체류기간 연장", "출입국에 방문해 연장 신청");
        Notice life = notice(3L, "교내 식당", "학생 식당 운영 시간");
        dorm.putTranslation(NoticeLang.EN, "Dormitory application", "Dormitory applications close February 10", NOW);
        given(loadNoticePort.findActiveForStudent(eq(5L), isNull(), any())).willReturn(List.of(dorm, visa, life));
        var adapter = new KeywordNoticeSearchAdapter(loadNoticePort);

        assertThat(adapter.findCandidates(5L, NoticeLang.KO, "기숙사 신청은 언제까지예요?", TODAY, 8))
                .extracting(Notice::getId).containsExactly(1L, 2L);   // 2번은 '신청'만 겹침
        assertThat(adapter.findCandidates(5L, NoticeLang.EN, "when do dormitory applications close", TODAY, 8))
                .extracting(Notice::getId).containsExactly(1L);
        assertThat(adapter.findCandidates(5L, NoticeLang.KO, "장학금", TODAY, 8)).isEmpty();
        assertThat(adapter.findCandidates(5L, NoticeLang.KO, "기숙사 신청", TODAY, 1)).hasSize(1);
    }

    @Test
    @DisplayName("토큰화: 2자 미만 라틴 단어는 버리고, 숫자와 CJK bigram 은 남긴다")
    void tokens() {
        assertThat(KeywordNoticeSearchAdapter.tokens("Apply by Feb 10, 기숙사!"))
                .contains("apply", "by", "feb", "10", "기숙", "숙사")
                .doesNotContain("a", "!");
    }
}
