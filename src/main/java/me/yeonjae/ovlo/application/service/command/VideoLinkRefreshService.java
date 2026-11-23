package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.result.VideoLinkRefreshResult;
import me.yeonjae.ovlo.application.port.in.university.RefreshVideoLinksUseCase;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore.DueLink;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 영상 링크 재확인.
 *
 * <p>일부러 트랜잭션으로 묶지 않는다. 링크 하나의 실패가 같은 회차의 다른 링크 갱신을 되돌리면
 * 같은 지점에서 매일 막힌다. 링크마다 따로 저장하고, 판정하지 못했거나 처리 중 예외가 난 링크도
 * 확인 시각을 남겨 대기열 맨 앞을 계속 차지하지 않게 한다.</p>
 *
 * <p>'사라짐'은 회차가 끝난 뒤 한꺼번에 반영한다. 한 회차에 사라짐이 {@code maxGonePerRun} 을 넘으면
 * 영상이 지워진 것이 아니라 원본 쪽이 이 서버를 막은 것으로 보고 하나도 숨기지 않는다 —
 * 그렇지 않으면 차단 한 번에 그 회차의 링크가 전부 화면에서 사라진다.</p>
 */
@Service
public class VideoLinkRefreshService implements RefreshVideoLinksUseCase {

    static final int MAX_TITLE = 500;
    static final int MAX_CHANNEL = 200;
    private static final Logger log = LoggerFactory.getLogger(VideoLinkRefreshService.class);

    private final VideoLinkStore store;
    private final VideoMetadataPort metadata;
    private final Clock clock;
    private final int batchSize;
    private final Duration pause;
    private final int maxGonePerRun;
    private final Duration maxRunTime;

    @Autowired
    public VideoLinkRefreshService(VideoLinkStore store, VideoMetadataPort metadata,
                                   @Value("${ovlo.video-links.refresh-batch-size:100}") int batchSize,
                                   @Value("${ovlo.video-links.refresh-pause-ms:1000}") long pauseMs,
                                   @Value("${ovlo.video-links.refresh-max-gone-per-run:10}") int maxGonePerRun,
                                   @Value("${ovlo.video-links.refresh-max-run-seconds:600}") long maxRunSeconds) {
        this(store, metadata, Clock.systemUTC(), batchSize, Duration.ofMillis(pauseMs), maxGonePerRun,
                Duration.ofSeconds(maxRunSeconds));
    }

    VideoLinkRefreshService(VideoLinkStore store, VideoMetadataPort metadata, Clock clock,
                            int batchSize, Duration pause, int maxGonePerRun, Duration maxRunTime) {
        this.store = store;
        this.metadata = metadata;
        this.clock = clock;
        this.batchSize = batchSize;
        this.pause = pause;
        this.maxGonePerRun = maxGonePerRun;
        this.maxRunTime = maxRunTime;
    }

    @Override
    public VideoLinkRefreshResult refreshDue() {
        List<DueLink> due = store.findDueForCheck(batchSize);
        Instant deadline = clock.instant().plus(maxRunTime);
        List<DueLink> gone = new ArrayList<>();
        int refreshed = 0;
        int undecided = 0;
        int failed = 0;
        boolean stoppedEarly = false;
        for (int i = 0; i < due.size(); i++) {
            DueLink link = due.get(i);
            try {
                VideoMetadata found = metadata.lookup(link.youtubeUrl());
                switch (found.status()) {
                    case FOUND -> {
                        String title = clean(found.title(), MAX_TITLE);
                        if (title == null || title.isEmpty()) {
                            store.markChecked(link.id(), clock.instant());
                            undecided++;
                        } else {
                            store.markAvailable(link.id(), title, clean(found.channel(), MAX_CHANNEL), clock.instant());
                            refreshed++;
                        }
                    }
                    case GONE -> gone.add(link);
                    case UNKNOWN -> {
                        store.markChecked(link.id(), clock.instant());
                        undecided++;
                    }
                    case THROTTLED -> stoppedEarly = true;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("영상 링크 재확인 실패 id={} : {}", link.id(), e.toString());
                pushBack(link);
            }
            if (stoppedEarly) {
                break;
            }
            boolean last = i == due.size() - 1;
            if (!last && (!clock.instant().isBefore(deadline) || !sleep())) {
                stoppedEarly = true;
                break;
            }
        }
        boolean suspicious = gone.size() > maxGonePerRun;
        int hidden = 0;
        for (DueLink link : gone) {
            try {
                if (suspicious) {
                    store.markChecked(link.id(), clock.instant());
                } else {
                    store.markUnavailable(link.id(), clock.instant());
                    hidden++;
                }
            } catch (RuntimeException e) {
                failed++;
                log.warn("영상 링크 숨김 처리 실패 id={} : {}", link.id(), e.toString());
            }
        }
        VideoLinkRefreshResult result = new VideoLinkRefreshResult(
                refreshed, hidden, undecided, failed, suspicious ? gone.size() : 0, stoppedEarly);
        if (suspicious) {
            log.warn("영상 링크 재확인: '사라짐' {}건이 상한 {}건을 넘어 숨기지 않았다 — 원본 쪽 차단인지 확인 필요. {}",
                    gone.size(), maxGonePerRun, result);
        } else {
            log.info("영상 링크 재확인: 대상 {} → {}", due.size(), result);
        }
        return result;
    }

    /** 처리 중 예외가 난 링크를 대기열 뒤로 보낸다. 이것마저 실패하면 다음 회차에 다시 맨 앞에 온다. */
    private void pushBack(DueLink link) {
        try {
            store.markChecked(link.id(), clock.instant());
        } catch (RuntimeException e) {
            log.warn("영상 링크 확인 시각 기록 실패 id={} : {}", link.id(), e.toString());
        }
    }

    private boolean sleep() {
        if (pause.isZero()) {
            return true;
        }
        try {
            Thread.sleep(pause.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** DB 가 받지 못하는 NUL 을 빼고, 컬럼 길이에 맞춰 자르되 이모지(서로게이트 쌍)를 반으로 가르지 않는다. */
    private static String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String text = value.replace("\u0000", "").strip();
        if (text.length() <= max) {
            return text;
        }
        int end = Character.isHighSurrogate(text.charAt(max - 1)) ? max - 1 : max;
        return text.substring(0, end);
    }
}
