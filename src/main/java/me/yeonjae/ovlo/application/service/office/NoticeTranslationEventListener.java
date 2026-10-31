package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.in.office.TranslateNoticeUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 승인 커밋 뒤 비동기로 번역을 돌린다. 승인 응답은 번역을 기다리지 않는다.
 * 예외는 호출자에게 전파되지 않으므로 여기서 기록한다. 실패 언어 알림은 유스케이스가 보낸다.
 */
@Component
public class NoticeTranslationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NoticeTranslationEventListener.class);

    private final TranslateNoticeUseCase translateNoticeUseCase;

    public NoticeTranslationEventListener(TranslateNoticeUseCase translateNoticeUseCase) {
        this.translateNoticeUseCase = translateNoticeUseCase;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(NoticeApprovedEvent event) {
        try {
            var outcome = translateNoticeUseCase.translateMissing(event.noticeId());
            log.info("[Notice] translated notice={} generated={} failed={}", event.noticeId(),
                    outcome.generated(), outcome.failed());
        } catch (RuntimeException e) {
            log.error("[Notice] translation job failed notice={}", event.noticeId(), e);
        }
    }
}
