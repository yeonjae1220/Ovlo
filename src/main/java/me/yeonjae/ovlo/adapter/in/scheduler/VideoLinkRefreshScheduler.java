package me.yeonjae.ovlo.adapter.in.scheduler;

import jakarta.annotation.PreDestroy;
import me.yeonjae.ovlo.application.port.in.university.RefreshVideoLinksUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 매일 03:25(서울) 영상 링크를 한 묶음씩 원본에 다시 확인한다. 묶음 크기 100 이면 600여 건이 약 주 1회 돈다.
 * {@code ovlo.video-links.refresh-enabled=false} 로 끄고 {@code ovlo.video-links.refresh-cron} 으로 조정한다.
 *
 * <p>한 회차가 외부 응답을 기다리며 몇 분씩 걸릴 수 있다. @Scheduled 작업은 스레드 하나를 나눠 쓰므로
 * 여기서 직접 돌리면 그동안 번역 재시도·개인정보 정리 같은 다른 작업이 멈춘다. 그래서 전용 스레드에 넘기고
 * 바로 돌아오며, 앞 회차가 아직 돌고 있으면 건너뛴다.</p>
 */
@Component
@ConditionalOnProperty(name = "ovlo.video-links.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class VideoLinkRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoLinkRefreshScheduler.class);

    private final RefreshVideoLinksUseCase refreshVideoLinksUseCase;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "video-link-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();

    public VideoLinkRefreshScheduler(RefreshVideoLinksUseCase refreshVideoLinksUseCase) {
        this.refreshVideoLinksUseCase = refreshVideoLinksUseCase;
    }

    @Scheduled(cron = "${ovlo.video-links.refresh-cron:0 25 3 * * *}", zone = "Asia/Seoul")
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("영상 링크 재확인: 앞 회차가 아직 돌고 있어 건너뛴다");
            return;
        }
        try {
            worker.execute(() -> {
                try {
                    refreshVideoLinksUseCase.refreshDue();
                } catch (RuntimeException e) {
                    log.error("영상 링크 재확인 회차 실패", e);
                } finally {
                    running.set(false);
                }
            });
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    @PreDestroy
    void shutdown() {
        worker.shutdownNow();
    }
}
