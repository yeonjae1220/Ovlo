package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.office.ManageConsultationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 00:20(서버 TZ) RESOLVED 뒤 7일 지난 상담을 CLOSED 로. */
@Component
public class ConsultationAutoCloseScheduler {

    private final ManageConsultationUseCase manageConsultationUseCase;

    public ConsultationAutoCloseScheduler(ManageConsultationUseCase manageConsultationUseCase) {
        this.manageConsultationUseCase = manageConsultationUseCase;
    }

    @Scheduled(cron = "${ovlo.office.consultation-auto-close-cron:0 20 0 * * *}")
    public void run() {
        manageConsultationUseCase.closeDue();
    }
}
