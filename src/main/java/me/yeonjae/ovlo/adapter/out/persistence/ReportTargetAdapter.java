package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.moderation.ReportTargetPort;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.OptionalLong;

/**
 * 신고 대상의 작성자를 찾고, 관리자 조치로 대상을 내린다.
 * 신고자가 화면에서 볼 수 없는 대상(지워진 글, 참여하지 않은 채팅방)은 없는 것으로 취급해 존재 여부를 드러내지 않는다.
 */
@Component
public class ReportTargetAdapter implements ReportTargetPort {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportTargetAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OptionalLong findAuthorVisibleTo(ReportTargetType type, long targetId, long reporterId) {
        String sql = switch (type) {
            case POST -> """
                    SELECT p.author_id FROM post p
                    WHERE p.id = :target AND NOT p.deleted AND NOT p.hidden_by_withdrawal
                    """;
            case COMMENT -> """
                    SELECT c.author_id FROM comment c JOIN post p ON p.id = c.post_id
                    WHERE c.id = :target AND NOT c.deleted AND NOT c.hidden_by_withdrawal
                      AND NOT p.deleted AND NOT p.hidden_by_withdrawal
                    """;
            case CHAT_MESSAGE -> """
                    SELECT m.sender_id FROM message m
                    JOIN chat_room_participant cp ON cp.chat_room_id = m.chat_room_id AND cp.member_id = :reporter
                    WHERE m.id = :target AND NOT m.hidden_by_withdrawal AND NOT m.hidden_by_moderation
                    """;
            case MEMBER -> "SELECT m.id FROM member m WHERE m.id = :target AND m.status <> 'WITHDRAWN'";
        };
        return jdbc.queryForList(sql, Map.of("target", targetId, "reporter", reporterId), Long.class).stream()
                .mapToLong(Long::longValue)
                .findFirst();
    }

    @Override
    public void removeContent(ReportTargetType type, long targetId) {
        String sql = switch (type) {
            // version 을 올려야 동시에 진행 중인 작성자의 수정 저장이 낙관적 락으로 실패한다(deleted=false 로 되돌리지 못하게).
            case POST -> "UPDATE post SET deleted = TRUE, version = version + 1 WHERE id = :target";
            case COMMENT -> "UPDATE comment SET deleted = TRUE WHERE id = :target";
            case CHAT_MESSAGE -> "UPDATE message SET hidden_by_moderation = TRUE WHERE id = :target";
            case MEMBER -> throw new IllegalArgumentException("회원 신고에는 내릴 콘텐츠가 없습니다");
        };
        jdbc.update(sql, Map.of("target", targetId));
    }
}
