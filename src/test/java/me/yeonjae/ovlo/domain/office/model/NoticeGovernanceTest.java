package me.yeonjae.ovlo.domain.office.model;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class NoticeGovernanceTest {
    private static final Instant NOW=Instant.parse("2026-11-13T00:00:00Z");
    private static final LocalDate TODAY=LocalDate.of(2026,11,13);
    Notice draft() { return Notice.register(1L,NoticeCategory.OTHER,"title","body","https://u.ac.kr/faq",List.of(),"2027-1",TODAY,TODAY.plusDays(90),1L,NOW); }
    Notice verified(boolean critical) {
        Notice n=draft(); n.configureSource(new NoticeSource(NoticeSource.Type.OFFICE_MATERIAL,"국제처","안내문","교환학생","전체",TODAY,null,1L,critical,true,null,List.of()),NOW);
        n.verifySource(TODAY); n.approve("1",NOW,TODAY); return n;
    }
    @Test void legacyApprovedMaterialIsNotGrounding() {
        Notice n=draft();n.approve("1",NOW,TODAY);assertThat(n.isGroundingEligible(TODAY,NoticeLang.KO)).isFalse();
    }
    @Test void criticalReviewExpiresAfterSeventhDay() {
        Notice n=verified(true);
        assertThat(n.isGroundingEligible(TODAY.plusDays(7),NoticeLang.KO)).isTrue();
        assertThat(n.isGroundingEligible(TODAY.plusDays(8),NoticeLang.KO)).isFalse();
    }
    @Test void sameSourceVerificationPreservesTranslationsAndVersion() {
        Notice n=verified(false); n.putTranslation(NoticeLang.EN,"Title","Body",NOW); int v=n.getVersion();
        n.verifySource(TODAY.plusDays(3)); assertThat(n.getVersion()).isEqualTo(v);
        assertThat(n.isGroundingEligible(TODAY.plusDays(3),NoticeLang.EN)).isTrue();
    }
    @Test void changingOnlyLinkAttachmentsOrDatesRequiresApproval() {
        Notice n=verified(false);int before=n.getVersion();n.putTranslation(NoticeLang.EN,"T","B",NOW);
        n.revise(n.getCategory(),n.getTitle(),n.getBody(),"https://u.ac.kr/new",List.of(3L),"2027-2",TODAY,TODAY.plusDays(120),NOW);
        assertThat(n.getVersion()).isEqualTo(before+1);assertThat(n.getStatus()).isEqualTo(NoticeStatus.DRAFT);
        assertThat(n.getTranslation(NoticeLang.EN).orElseThrow().isStaleFor(n.getVersion())).isTrue();
        assertThat(n.sourceEligible(TODAY)).isFalse();
    }
    @Test void holdImmediatelyPreventsUseAndStaleRevisionFails() {
        Notice n=verified(false);n.holdSource("충돌");assertThat(n.isGroundingEligible(TODAY,NoticeLang.KO)).isFalse();
        assertThatThrownBy(() -> n.expectRevision(99)).isInstanceOf(RuntimeException.class);
    }
    @Test void officeMidnightIsKoreanMidnight() {
        var clock=Clock.fixed(Instant.parse("2026-11-13T15:00:00Z"),ZoneOffset.UTC);
        assertThat(me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock)).isEqualTo(LocalDate.of(2026,11,14));
    }
}
