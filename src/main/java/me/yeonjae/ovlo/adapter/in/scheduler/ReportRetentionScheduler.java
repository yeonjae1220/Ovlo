package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.moderation.AnonymizeExpiredReportsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 00:30(서버 TZ) 보유 기간이 지난 신고 기록을 익명화한다. */
@Component
public class ReportRetentionScheduler {

    private final AnonymizeExpiredReportsUseCase useCase;

    public ReportRetentionScheduler(AnonymizeExpiredReportsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(cron = "${ovlo.moderation.report-retention-cron:0 30 0 * * *}")
    public void run() {
        useCase.anonymizeExpired();
    }
}
