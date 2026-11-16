package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="ovlo.office.translation-retry-enabled",havingValue="true",matchIfMissing=true)
public class NoticeTranslationScheduler {
    private final TranslateNoticeUseCase translations;
    public NoticeTranslationScheduler(TranslateNoticeUseCase translations) {
        this.translations=translations;
    }
    @Scheduled(fixedDelayString="${ovlo.office.translation-retry-delay-ms:10000}", initialDelayString="${ovlo.office.translation-retry-initial-delay-ms:30000}")
    public void run() {
        translations.retryDue();
    }
}
