package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.result.VideoLinkRefreshResult;
import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort.Status;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort.VideoMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class VideoLinkRefreshServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T00:00:00Z");

    private final FakeStore store = new FakeStore();

    @Test
    @DisplayName("원본에 있는 링크는 제목·채널명을 갱신하고, 사라진 링크는 숨기고, 판정 못 한 링크는 확인 시각만 남긴다")
    void appliesEachOutcome() {
        store.due(1, 2, 3);
        VideoLinkRefreshResult result = service(url -> switch (url) {
            case "u1" -> new VideoMetadata(Status.FOUND, "  새 제목  ", "새 채널");
            case "u2" -> VideoMetadata.of(Status.GONE);
            default -> VideoMetadata.of(Status.UNKNOWN);
        }).refreshDue();

        // 숨김은 회차가 끝난 뒤에 반영한다
        assertThat(store.events).containsExactly("available 1 새 제목|새 채널", "checked 3", "unavailable 2");
        assertThat(result).isEqualTo(new VideoLinkRefreshResult(1, 1, 1, 0, 0, false));
    }

    @Test
    @DisplayName("한 링크에서 예외가 나도 나머지 링크는 계속 처리한다")
    void oneFailureDoesNotStopTheBatch() {
        store.due(1, 2, 3);
        VideoLinkRefreshResult result = service(url -> {
            if (url.equals("u2")) {
                throw new IllegalStateException("boom");
            }
            return new VideoMetadata(Status.FOUND, "제목", "채널");
        }).refreshDue();

        // 실패한 링크는 확인 시각만 남겨 다음 회차에 맨 앞을 다시 차지하지 않게 한다
        assertThat(store.events).containsExactly("available 1 제목|채널", "checked 2", "available 3 제목|채널");
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.refreshed()).isEqualTo(2);
    }

    @Test
    @DisplayName("원본이 요청을 제한하면 그 자리에서 멈추고 남은 링크는 건드리지 않는다")
    void stopsWhenThrottled() {
        store.due(1, 2, 3);
        VideoLinkRefreshResult result = service(url -> url.equals("u2")
                ? VideoMetadata.of(Status.THROTTLED)
                : new VideoMetadata(Status.FOUND, "제목", "채널")).refreshDue();

        assertThat(store.events).containsExactly("available 1 제목|채널");
        assertThat(result.stoppedEarly()).isTrue();
    }

    @Test
    @DisplayName("있다고 하면서 제목이 비어 오면 기존 제목을 지우지 않고 확인 시각만 남긴다")
    void blankTitleIsNotWritten() {
        store.due(1);
        service(url -> new VideoMetadata(Status.FOUND, "  ", "채널")).refreshDue();

        assertThat(store.events).containsExactly("checked 1");
    }

    @Test
    @DisplayName("저장이 실패한 링크도 확인 시각을 남기고 다음 링크로 넘어간다")
    void storeFailureIsPushedBack() {
        store.due(1, 2);
        store.failAvailableFor = 1L;
        VideoLinkRefreshResult result = service(url -> new VideoMetadata(Status.FOUND, "제목", "채널")).refreshDue();

        assertThat(store.events).containsExactly("checked 1", "available 2 제목|채널");
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    @DisplayName("'사라짐'이 한 회차 상한을 넘으면 원본 쪽 차단으로 보고 하나도 숨기지 않는다")
    void manyGoneResponsesHideNothing() {
        store.due(1, 2, 3, 4);
        VideoLinkRefreshResult result = service(url -> url.equals("u4")
                ? new VideoMetadata(Status.FOUND, "제목", "채널")
                : VideoMetadata.of(Status.GONE), 2).refreshDue();

        assertThat(store.events).containsExactly("available 4 제목|채널", "checked 1", "checked 2", "checked 3");
        assertThat(result.hidden()).isZero();
        assertThat(result.goneIgnored()).isEqualTo(3);
    }

    @Test
    @DisplayName("'사라짐'이 상한 이하면 회차 끝에 숨긴다")
    void fewGoneResponsesAreHidden() {
        store.due(1, 2, 3);
        VideoLinkRefreshResult result = service(url -> url.equals("u3")
                ? new VideoMetadata(Status.FOUND, "제목", "채널")
                : VideoMetadata.of(Status.GONE), 2).refreshDue();

        assertThat(store.events).containsExactly("available 3 제목|채널", "unavailable 1", "unavailable 2");
        assertThat(result.hidden()).isEqualTo(2);
        assertThat(result.goneIgnored()).isZero();
    }

    @Test
    @DisplayName("회차 시간 상한을 넘기면 남은 링크는 다음 회차로 미룬다")
    void stopsAtTimeBudget() {
        store.due(1, 2, 3);
        java.util.concurrent.atomic.AtomicLong seconds = new java.util.concurrent.atomic.AtomicLong();
        Clock ticking = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return NOW.plusSeconds(seconds.get()); }
        };
        VideoMetadataPort port = url -> {
            seconds.addAndGet(400);   // 응답이 느린 원본
            return new VideoMetadata(Status.FOUND, "제목", "채널");
        };
        VideoLinkRefreshResult result = new VideoLinkRefreshService(store, port, ticking, 100, Duration.ZERO, 10,
                Duration.ofSeconds(600)).refreshDue();

        assertThat(store.titles.keySet()).containsExactly(1L, 2L);
        assertThat(result.stoppedEarly()).isTrue();
    }

    @Test
    @DisplayName("쉬는 중 인터럽트되면 멈추고 인터럽트 표시를 되돌려 둔다")
    void interruptedWhilePausing() {
        store.due(1, 2);
        VideoMetadataPort port = url -> {
            Thread.currentThread().interrupt();
            return new VideoMetadata(Status.FOUND, "제목", "채널");
        };
        VideoLinkRefreshResult result = new VideoLinkRefreshService(store, port, Clock.fixed(NOW, ZoneOffset.UTC), 100,
                Duration.ofMillis(50), 10, Duration.ofSeconds(600)).refreshDue();

        assertThat(Thread.interrupted()).isTrue();   // 표시를 확인하면서 지운다
        assertThat(store.titles.keySet()).containsExactly(1L);
        assertThat(result.stoppedEarly()).isTrue();
    }

    @Test
    @DisplayName("NUL 문자는 빼고, 길이 경계에서 이모지를 반으로 가르지 않는다")
    void cleansText() {
        store.due(1);
        String emojiAtEdge = "가".repeat(VideoLinkRefreshService.MAX_TITLE - 1) + "\uD83C\uDF93";
        service(url -> new VideoMetadata(Status.FOUND, emojiAtEdge, "채\u0000널")).refreshDue();

        assertThat(store.titles.get(1L)).isEqualTo("가".repeat(VideoLinkRefreshService.MAX_TITLE - 1));
        assertThat(store.channels.get(1L)).isEqualTo("채널");
    }

    @Test
    @DisplayName("컬럼 길이를 넘는 제목·채널명은 잘라서 저장한다")
    void longValuesAreCut() {
        store.due(1);
        service(url -> new VideoMetadata(Status.FOUND, "가".repeat(600), "나".repeat(300))).refreshDue();

        assertThat(store.titles.get(1L)).hasSize(VideoLinkRefreshService.MAX_TITLE);
        assertThat(store.channels.get(1L)).hasSize(VideoLinkRefreshService.MAX_CHANNEL);
    }

    private VideoLinkRefreshService service(Function<String, VideoMetadata> lookup) {
        return service(lookup, 10);
    }

    private VideoLinkRefreshService service(Function<String, VideoMetadata> lookup, int maxGonePerRun) {
        VideoMetadataPort port = lookup::apply;
        return new VideoLinkRefreshService(store, port, Clock.fixed(NOW, ZoneOffset.UTC), 100, Duration.ZERO,
                maxGonePerRun, Duration.ofSeconds(600));
    }

    private static final class FakeStore implements VideoLinkStore {
        private final List<DueLink> due = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private final Map<Long, String> titles = new LinkedHashMap<>();
        private final Map<Long, String> channels = new LinkedHashMap<>();
        private Long failAvailableFor;

        void due(long... ids) {
            for (long id : ids) {
                due.add(new DueLink(id, "u" + id));
            }
        }

        @Override public List<VideoLinkResult> findByGlobalUniversity(long globalUnivId, int limit, int offset) { return List.of(); }
        @Override public long countByGlobalUniversity(long globalUnivId) { return 0; }
        @Override public List<DueLink> findDueForCheck(int limit) { return due; }

        @Override
        public void markAvailable(long id, String title, String channel, Instant checkedAt) {
            if (failAvailableFor != null && failAvailableFor == id) {
                throw new IllegalStateException("db down");
            }
            titles.put(id, title);
            channels.put(id, channel);
            events.add("available " + id + " " + title + "|" + channel);
        }

        @Override public void markUnavailable(long id, Instant checkedAt) { events.add("unavailable " + id); }
        @Override public void markChecked(long id, Instant checkedAt) { events.add("checked " + id); }
    }
}
