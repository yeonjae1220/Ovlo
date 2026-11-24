package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.member.LegacyMemberErasurePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name="ovlo.privacy.legacy-erasure-enabled",havingValue="true")
public class LegacyMemberErasureService {
    private final LegacyMemberErasurePort erasure;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(LegacyMemberErasureService.class);
    public LegacyMemberErasureService(LegacyMemberErasurePort erasure) { this.erasure=erasure; }
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void erase() {
        int count=erasure.eraseBatch(50);
        if(count>0) log.info("Erased legacy withdrawn identities count={} remaining={}",count,erasure.pendingCount());
    }
}
