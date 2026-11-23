package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;

import java.time.Instant;
import java.util.List;

public interface VideoLinkStore {

    List<VideoLinkResult> findByGlobalUniversity(long globalUnivId, int limit, int offset);

    long countByGlobalUniversity(long globalUnivId);

    /** 확인한 적 없는 링크 먼저, 그다음 확인한 지 오래된 순. 숨긴 링크도 포함한다(되살아났는지 보려고). */
    List<DueLink> findDueForCheck(int limit);

    /** 원본에 있다 — 제목·채널명을 갱신하고 숨김을 푼다. */
    void markAvailable(long id, String title, String channel, Instant checkedAt);

    /** 원본에서 사라졌다 — 처음 본 시각을 남기고 숨긴다. */
    void markUnavailable(long id, Instant checkedAt);

    /** 판정하지 못했다 — 확인 시각만 남겨 대기열 뒤로 보낸다. */
    void markChecked(long id, Instant checkedAt);

    record DueLink(long id, String youtubeUrl) {}
}
