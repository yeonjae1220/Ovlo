package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** exchange_video_reviews 를 '영상 링크'로 읽고 최신성 컬럼을 갱신한다. */
@Repository
public class VideoLinkPersistenceAdapter implements VideoLinkStore {

    /**
     * 그 대학과, 그 대학으로 숨긴 중복 행에 달린 교환대학 행의 영상 중 숨기지 않은 것.
     * 카탈로그와 같은 기준으로, 요청한 id 가 숨긴 중복 행이거나 비활성 대학이면 아무것도 주지 않는다.
     */
    private static final String OF_UNIVERSITY = """
            FROM exchange_video_reviews v
            JOIN exchange_universities e ON e.id = v.university_id
            JOIN global_universities g ON g.id = e.global_univ_id
            WHERE (g.id = :globalUnivId OR g.canonical_id = :globalUnivId)
              AND v.unavailable_since IS NULL
              AND EXISTS (SELECT 1 FROM global_universities r
                          WHERE r.id = :globalUnivId
                            AND r.canonical_id IS NULL AND r.inactive_reason IS NULL)
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public VideoLinkPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<VideoLinkResult> findByGlobalUniversity(long globalUnivId, int limit, int offset) {
        return jdbc.query(
                "SELECT v.id, v.youtube_url, v.title, v.channel, v.published_at " + OF_UNIVERSITY
                        + " ORDER BY v.published_at DESC NULLS LAST, v.id DESC LIMIT :limit OFFSET :offset",
                Map.of("globalUnivId", globalUnivId, "limit", limit, "offset", offset),
                (rs, row) -> new VideoLinkResult(
                        rs.getLong("id"), rs.getString("youtube_url"), rs.getString("title"),
                        rs.getString("channel"), rs.getObject("published_at", OffsetDateTime.class)));
    }

    @Override
    public long countByGlobalUniversity(long globalUnivId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) " + OF_UNIVERSITY,
                Map.of("globalUnivId", globalUnivId), Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public List<DueLink> findDueForCheck(int limit) {
        return jdbc.query("""
                        SELECT id, youtube_url FROM exchange_video_reviews
                        ORDER BY last_checked_at NULLS FIRST, id
                        LIMIT :limit
                        """,
                Map.of("limit", limit),
                (rs, row) -> new DueLink(rs.getLong("id"), rs.getString("youtube_url")));
    }

    @Override
    public void markAvailable(long id, String title, String channel, Instant checkedAt) {
        jdbc.update("""
                        UPDATE exchange_video_reviews
                        SET title = :title, channel = COALESCE(:channel, channel),
                            last_checked_at = :at, unavailable_since = NULL
                        WHERE id = :id
                        """,
                new MapSqlParameterSource().addValue("id", id).addValue("title", title)
                        .addValue("channel", channel).addValue("at", Timestamp.from(checkedAt)));
    }

    @Override
    public void markUnavailable(long id, Instant checkedAt) {
        jdbc.update("""
                        UPDATE exchange_video_reviews
                        SET last_checked_at = :at, unavailable_since = COALESCE(unavailable_since, :at)
                        WHERE id = :id
                        """,
                Map.of("id", id, "at", Timestamp.from(checkedAt)));
    }

    @Override
    public void markChecked(long id, Instant checkedAt) {
        jdbc.update("UPDATE exchange_video_reviews SET last_checked_at = :at WHERE id = :id",
                Map.of("id", id, "at", Timestamp.from(checkedAt)));
    }
}
