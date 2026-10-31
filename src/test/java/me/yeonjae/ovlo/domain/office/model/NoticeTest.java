package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoticeTest {

    private static final Instant NOW = Instant.parse("2027-02-01T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 2, 1);
    private static final LocalDate FROM = LocalDate.of(2027, 2, 1);
    private static final LocalDate UNTIL = LocalDate.of(2027, 3, 15);

    private static Notice draft() {
        return Notice.register(5L, NoticeCategory.HOUSING, "기숙사 신청 안내", "2027-1학기 기숙사 신청은 2월 10일까지입니다.",
                "https://univ.ac.kr/dorm", List.of(), "2027-1", FROM, UNTIL, 11L, NOW);
    }

    private static Notice approved() {
        Notice n = draft();
        n.approve("11", NOW, TODAY);
        return n;
    }

    @Nested
    class Register {
        @Test
        @DisplayName("등록하면 DRAFT, version 1, 원문 언어 ko, 번역 없음")
        void registersDraft() {
            Notice n = draft();

            assertThat(n.getStatus()).isEqualTo(NoticeStatus.DRAFT);
            assertThat(n.getVersion()).isEqualTo(1);
            assertThat(n.getSourceLang()).isEqualTo(NoticeLang.KO);
            assertThat(n.getTranslations()).isEmpty();
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.KO)).isFalse();
        }

        @Test
        @DisplayName("제목·본문이 비었거나 만료일이 시작일보다 앞서면 거부")
        void rejectsInvalid() {
            assertThatThrownBy(() -> Notice.register(5L, NoticeCategory.OTHER, " ", "b", null, List.of(), "2027-1",
                    FROM, UNTIL, 11L, NOW)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Notice.register(5L, NoticeCategory.OTHER, "t", " ", null, List.of(), "2027-1",
                    FROM, UNTIL, 11L, NOW)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Notice.register(5L, NoticeCategory.OTHER, "t", "b", null, List.of(), "2027-1",
                    UNTIL, FROM, 11L, NOW)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Approve {
        @Test
        @DisplayName("DRAFT 를 승인하면 APPROVED 가 되고 승인자·시각이 남는다")
        void approvesDraft() {
            Notice n = draft();

            n.approve("11", NOW, TODAY);

            assertThat(n.getStatus()).isEqualTo(NoticeStatus.APPROVED);
            assertThat(n.getApprovedBy()).isEqualTo("11");
            assertThat(n.getApprovedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("만료일이 오늘보다 앞서면 승인할 수 없다 (지난 공지를 근거로 쓰지 않는다)")
        void rejectsPastExpiry() {
            Notice n = draft();

            assertThatThrownBy(() -> n.approve("11", NOW, UNTIL.plusDays(1)))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getErrorType())
                    .isEqualTo(NoticeException.ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("APPROVED·ARCHIVED 는 다시 승인할 수 없다 (INVALID_STATE)")
        void rejectsWrongState() {
            Notice n = approved();
            assertThatThrownBy(() -> n.approve("11", NOW, TODAY))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getErrorType())
                    .isEqualTo(NoticeException.ErrorType.INVALID_STATE);

            n.archive();
            assertThatThrownBy(() -> n.approve("11", NOW, TODAY)).isInstanceOf(NoticeException.class);
        }

        @Test
        @DisplayName("EXPIRED 는 새 만료일을 준 뒤에만 다시 승인된다")
        void reapproveExpiredNeedsNewExpiry() {
            Notice n = approved();
            assertThat(n.expire(UNTIL.plusDays(1))).isTrue();
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.EXPIRED);

            assertThatThrownBy(() -> n.approve("11", NOW, UNTIL.plusDays(1))).isInstanceOf(NoticeException.class);

            n.reschedule(FROM, UNTIL.plusMonths(6), NOW);
            n.approve("11", NOW, UNTIL.plusDays(1));
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        }
    }

    @Nested
    class Expire {
        @Test
        @DisplayName("만료일 당일까지는 유효하고 다음 날부터 EXPIRED")
        void expiresAfterExpiryDate() {
            Notice n = approved();

            assertThat(n.expire(UNTIL)).isFalse();
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.APPROVED);
            assertThat(n.expire(UNTIL.plusDays(1))).isTrue();
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.EXPIRED);
        }

        @Test
        @DisplayName("DRAFT·ARCHIVED 는 만료 대상이 아니다")
        void onlyApprovedExpires() {
            Notice d = draft();
            assertThat(d.expire(UNTIL.plusDays(1))).isFalse();
            assertThat(d.getStatus()).isEqualTo(NoticeStatus.DRAFT);
        }
    }

    @Nested
    class EditAndVersion {
        @Test
        @DisplayName("본문을 고치면 version+1, DRAFT 로 돌아가고 기존 번역은 stale 이 된다")
        void contentEditResetsApproval() {
            Notice n = approved();
            n.putTranslation(NoticeLang.EN, "Dorm application", "Apply by Feb 10.", NOW);
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.EN)).isTrue();

            n.editContent(NoticeCategory.HOUSING, "기숙사 신청 안내", "신청은 2월 12일까지입니다.", "https://univ.ac.kr/dorm",
                    List.of(), NOW);

            assertThat(n.getVersion()).isEqualTo(2);
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.DRAFT);
            assertThat(n.getTranslation(NoticeLang.EN).orElseThrow().isStaleFor(n.getVersion())).isTrue();
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.EN)).isFalse();
            assertThat(n.translationsToGenerate(Set.of(NoticeLang.KO, NoticeLang.EN, NoticeLang.JA)))
                    .containsExactlyInAnyOrder(NoticeLang.EN, NoticeLang.JA);
        }

        @Test
        @DisplayName("내용이 같으면 version 과 상태가 그대로다")
        void sameContentKeepsVersion() {
            Notice n = approved();

            n.editContent(NoticeCategory.HOUSING, "기숙사 신청 안내", "2027-1학기 기숙사 신청은 2월 10일까지입니다.",
                    "https://univ.ac.kr/dorm", List.of(), NOW);

            assertThat(n.getVersion()).isEqualTo(1);
            assertThat(n.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        }

        @Test
        @DisplayName("보관된 공지는 수정·승인·번역할 수 없다")
        void archivedIsFrozen() {
            Notice n = approved();
            n.archive();

            assertThatThrownBy(() -> n.editContent(NoticeCategory.OTHER, "x", "y", null, List.of(), NOW))
                    .isInstanceOf(NoticeException.class);
            assertThatThrownBy(() -> n.putTranslation(NoticeLang.EN, "x", "y", NOW)).isInstanceOf(NoticeException.class);
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.KO)).isFalse();
        }
    }

    @Nested
    class Grounding {
        @Test
        @DisplayName("원문 언어(ko)는 번역 없이도, 다른 언어는 최신 번역이 있어야 근거 자격이 있다")
        void eligibilityPerLanguage() {
            Notice n = approved();

            assertThat(n.isGroundingEligible(TODAY, NoticeLang.KO)).isTrue();
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.EN)).isFalse();

            n.putTranslation(NoticeLang.EN, "Dorm application", "Apply by Feb 10.", NOW);
            assertThat(n.isGroundingEligible(TODAY, NoticeLang.EN)).isTrue();
        }

        @Test
        @DisplayName("시작일 전·만료일 후에는 근거 자격이 없다")
        void eligibilityWindow() {
            Notice n = approved();

            assertThat(n.isGroundingEligible(FROM.minusDays(1), NoticeLang.KO)).isFalse();
            assertThat(n.isGroundingEligible(UNTIL, NoticeLang.KO)).isTrue();
            assertThat(n.isGroundingEligible(UNTIL.plusDays(1), NoticeLang.KO)).isFalse();
        }

        @Test
        @DisplayName("검토(REVIEWED) 표시는 최신 번역에만 가능하다")
        void reviewOnlyFresh() {
            Notice n = approved();
            n.putTranslation(NoticeLang.EN, "Dorm", "Apply by Feb 10.", NOW);
            n.reviewTranslation(NoticeLang.EN);
            assertThat(n.getTranslation(NoticeLang.EN).orElseThrow().getStatus()).isEqualTo(TranslationStatus.REVIEWED);

            n.editContent(NoticeCategory.HOUSING, "기숙사 신청 안내", "바뀐 본문", null, List.of(), NOW);
            assertThatThrownBy(() -> n.reviewTranslation(NoticeLang.EN)).isInstanceOf(NoticeException.class);
            assertThatThrownBy(() -> n.reviewTranslation(NoticeLang.JA)).isInstanceOf(NoticeException.class);
        }
    }
}
