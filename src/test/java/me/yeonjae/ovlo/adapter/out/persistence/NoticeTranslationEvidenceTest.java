package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.service.office.NoticeTranslationEvidence;
import me.yeonjae.ovlo.domain.office.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NoticeTranslationEvidenceTest {
    Notice notice() {
        return Notice.register(1L,NoticeCategory.OTHER,"교육 안내","교육은 1.5시간입니다.",null,List.of(),"2026-2",
                LocalDate.now().minusDays(1),LocalDate.now().plusDays(30),2L,Instant.now());
    }
    @Test void fingerprintSurvivesJsonAndReviewButRejectsChangedOrQuarantinedText() {
        var n=notice();n.putTranslation(NoticeLang.EN,"Training","Training lasts 1.5 hours.",Instant.now());
        String hash=NoticeTranslationEvidence.fingerprint(n,NoticeLang.EN);
        var citation=new NoticeCitation(1,1L,n.getVersion(),"Training",null,NoticeSource.Type.OFFICE_MATERIAL,"Office",LocalDate.now(),n.getExpiresAt(),hash);
        assertThat(NoticeJson.read(NoticeJson.write(citation),NoticeCitation.class)).isEqualTo(citation);
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.EN,hash)).isTrue();
        n.reviewTranslation(NoticeLang.EN);
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.EN,hash)).isTrue();
        n.putTranslation(NoticeLang.DE,"Schulung","1,5 Stunden",Instant.now());
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.EN,hash)).isTrue();
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.EN,null)).isFalse();
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.KO,null)).isTrue();
        n.putTranslation(NoticeLang.EN,"Training","Training lasts 15 hours.",Instant.now());
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.EN,hash)).isFalse();
        assertThat(NoticeTranslationEvidence.matches(n,NoticeLang.FR,hash)).isFalse();
    }
    @Test void legacyJsonRemainsReadableWithoutInventingAHash() {
        String legacy="""
                {"index":1,"noticeId":2,"version":1,"title":"Training","sourceUrl":null,
                 "sourceType":"OFFICE_MATERIAL","department":"Office","verifiedOn":"2026-11-16","expiresAt":"2026-12-16"}
                """;
        assertThat(NoticeJson.read(legacy,NoticeCitation.class).translationFingerprint()).isNull();
    }
}
