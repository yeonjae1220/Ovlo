package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.in.moderation.AnonymizeExpiredReportsUseCase;
import me.yeonjae.ovlo.application.port.out.moderation.ReportRetentionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Period;
import java.time.ZoneOffset;

/** 개인정보처리방침의 '신고 기록: 처리 후 1년' 을 지키는 보유 기간 작업. */
@Service
public class ReportRetentionService implements AnonymizeExpiredReportsUseCase {

    static final Period RETENTION = Period.ofYears(1);
    private static final Logger log = LoggerFactory.getLogger(ReportRetentionService.class);

    private final ReportRetentionPort port;
    private final Clock clock;

    public ReportRetentionService(ReportRetentionPort port, Clock clock) {
        this.port = port;
        this.clock = clock;
    }

    @Override
    public int anonymizeExpired() {
        var cutoff = clock.instant().atOffset(ZoneOffset.UTC).minus(RETENTION).toInstant();
        int count = port.anonymizeOlderThan(cutoff);
        if (count > 0) log.info("[ReportRetention] anonymized={} cutoff={}", count, cutoff);
        return count;
    }
}
