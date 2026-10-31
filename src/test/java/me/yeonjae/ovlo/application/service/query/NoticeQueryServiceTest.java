package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.StudentNoticeResult;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoticeQueryServiceTest {

    private static final Instant NOW = Instant.parse("2027-02-05T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 2, 5);

    @Mock LoadNoticePort loadNoticePort;

    private NoticeQueryService sut;
    private Notice notice;

    @BeforeEach
    void setUp() {
        sut = new NoticeQueryService(loadNoticePort, Clock.fixed(NOW, ZoneOffset.UTC));
        notice = Notice.register(5L, NoticeCategory.HOUSING, "기숙사 신청", "2월 10일까지", null, List.of(), "2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 15), 11L, NOW);
        notice.assignId(77L);
        notice.approve("11", NOW, TODAY);
        notice.putTranslation(NoticeLang.EN, "Dorm application", "By Feb 10", NOW);
    }

    @Test
    @DisplayName("요청 언어의 최신 번역이 있으면 그것을, 없으면 원문(ko)으로 폴백해 보여 준다")
    void languageFallback() {
        given(loadNoticePort.findActiveForStudent(5L, null, TODAY)).willReturn(List.of(notice));

        List<StudentNoticeResult> en = sut.findForStudent(5L, null, NoticeLang.EN);
        List<StudentNoticeResult> vi = sut.findForStudent(5L, null, NoticeLang.VI);

        assertThat(en).singleElement().satisfies(r -> {
            assertThat(r.lang()).isEqualTo(NoticeLang.EN);
            assertThat(r.title()).isEqualTo("Dorm application");
            assertThat(r.machineTranslated()).isTrue();
        });
        assertThat(vi).singleElement().satisfies(r -> {
            assertThat(r.lang()).isEqualTo(NoticeLang.KO);
            assertThat(r.title()).isEqualTo("기숙사 신청");
            assertThat(r.machineTranslated()).isFalse();
        });
    }

    @Test
    @DisplayName("stale 번역은 원문으로 폴백하고, 검토된 번역은 기계 번역 표시가 없다")
    void staleAndReviewed() {
        notice.reviewTranslation(NoticeLang.EN);
        given(loadNoticePort.findById(77L)).willReturn(Optional.of(notice));
        assertThat(sut.findStudentDetail(5L, 77L, NoticeLang.EN)).hasValueSatisfying(r ->
                assertThat(r.machineTranslated()).isFalse());

        notice.editContent(NoticeCategory.HOUSING, "기숙사 신청", "2월 12일까지", null, List.of(), NOW);
        notice.approve("11", NOW, TODAY);
        assertThat(sut.findStudentDetail(5L, 77L, NoticeLang.EN)).hasValueSatisfying(r ->
                assertThat(r.lang()).isEqualTo(NoticeLang.KO));
    }

    @Test
    @DisplayName("다른 Office 의 공지나 유효하지 않은 공지는 상세에서 비어 있다")
    void hiddenWhenNotActiveOrOtherOffice() {
        given(loadNoticePort.findById(77L)).willReturn(Optional.of(notice));
        assertThat(sut.findStudentDetail(99L, 77L, NoticeLang.KO)).isEmpty();

        notice.archive();
        assertThat(sut.findStudentDetail(5L, 77L, NoticeLang.KO)).isEmpty();
    }
}
