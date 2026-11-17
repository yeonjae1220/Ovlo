package me.yeonjae.ovlo.adapter.in.scheduler;

import me.yeonjae.ovlo.application.port.in.office.EraseExpiredOfficeDataUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="ovlo.office.personal-data-cleanup-enabled",havingValue="true",matchIfMissing=true)
public class OfficePersonalDataScheduler {
    private final EraseExpiredOfficeDataUseCase data;
    public OfficePersonalDataScheduler(EraseExpiredOfficeDataUseCase data) { this.data=data; }
    @Scheduled(fixedDelayString="${ovlo.office.personal-data-cleanup-delay-ms:60000}")
    public void run() { data.eraseExpiredBatch(); }
}
