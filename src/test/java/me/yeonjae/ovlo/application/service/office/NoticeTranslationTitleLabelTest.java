package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class NoticeTranslationTitleLabelTest {
    NoticeTranslationGate.Verdict check(String source,String target) {
        return new NoticeTranslationGate().check(new TranslationPort.Request(NoticeLang.KO,NoticeLang.EN,source,"교육은 2시간입니다."),
                new TranslationPort.Result(target,"Training lasts 2 hours.","test"));
    }
    @ParameterizedTest @ValueSource(strings={"[운영 검증 전용]","［운영 검증 전용］","【운영 검증 전용】","[중요]","[검증용]","[교환학생 안내]"})
    void untranslatedDescriptiveLabelIsAlwaysRejected(String label) {
        assertThat(check(label+" 교육 안내 나",label+" Training 나").passed()).isFalse();
    }
    @Test void copiedLabelCannotHideBehindChangedSpacingOrRemovedBrackets() {
        assertThat(check("[운영 검증 전용] 교육","운영\u00a0검증 전용 Training").passed()).isFalse();
    }
    @Test void translatedLabelAndUntranslatedSectionIdentifierAreAccepted() {
        assertThat(check("[운영 검증 전용] 교육 안내 나","[For operational verification only] Training 나").passed()).isTrue();
    }
    @Test void arbitraryIdentifiersAndProperNamesAreNotBlanketRejected() {
        assertThat(check("[OVLO-2026] 가나대학교 교육 나","[OVLO-2026] 가나대학교 Training 나").passed()).isTrue();
        assertThat(check("[중요-2026] 교육 나","[중요-2026] Training 나").passed()).isTrue();
        assertThat(check("[중요] 중요한대학교 교육","[Important] 중요한대학교 Training").passed()).isTrue();
    }
}
