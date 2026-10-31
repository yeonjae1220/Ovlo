package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.office.ExpireNoticesUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 00:10(서버 TZ) 공지 만료 처리와 만료 임박 알림. {@code ovlo.office.notice-expiry-cron} 으로 조정. */
@Component
public class NoticeExpiryScheduler {

    private final ExpireNoticesUseCase expireNoticesUseCase;

    public NoticeExpiryScheduler(ExpireNoticesUseCase expireNoticesUseCase) {
        this.expireNoticesUseCase = expireNoticesUseCase;
    }

    @Scheduled(cron = "${ovlo.office.notice-expiry-cron:0 10 0 * * *}")
    public void run() {
        expireNoticesUseCase.expireDue();
        expireNoticesUseCase.warnExpiringSoon();
    }
}
