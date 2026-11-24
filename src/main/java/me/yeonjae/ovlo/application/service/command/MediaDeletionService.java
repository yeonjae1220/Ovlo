package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.port.out.media.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="ovlo.media.cleanup-enabled",havingValue="true",matchIfMissing=true)
public class MediaDeletionService {
    private static final Logger log = LoggerFactory.getLogger(MediaDeletionService.class);
    private final MediaDeletionPort jobs;
    private final StoragePort storage;
    public MediaDeletionService(MediaDeletionPort jobs, StoragePort storage) { this.jobs=jobs; this.storage=storage; }
    @Scheduled(fixedDelayString="${ovlo.media.cleanup-delay-ms:60000}", initialDelayString="${ovlo.media.cleanup-delay-ms:60000}")
    public void clean() {
        for (var job : jobs.claim(50)) {
            try {
                storage.delete(job.path()); // outside the DB transaction; idempotent after a crash
                jobs.complete(job.mediaId());
            } catch (RuntimeException ex) {
                log.warn("Media deletion will retry id={} error={}", job.mediaId(), ex.getClass().getSimpleName());
            }
        }
    }
}
