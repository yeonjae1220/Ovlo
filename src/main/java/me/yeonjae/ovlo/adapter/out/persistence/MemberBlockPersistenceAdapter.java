package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.result.BlockedMemberResult;
import me.yeonjae.ovlo.application.port.out.moderation.MemberBlockPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MemberBlockPersistenceAdapter implements MemberBlockPort {

    private final NamedParameterJdbcTemplate jdbc;

    public MemberBlockPersistenceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void block(long actorId, long targetId) {
        jdbc.update("INSERT INTO member_block(actor_id, target_id) VALUES(:a, :b) ON CONFLICT DO NOTHING",
                Map.of("a", actorId, "b", targetId));
    }

    @Override
    public void unblock(long actorId, long targetId) {
        jdbc.update("DELETE FROM member_block WHERE actor_id = :a AND target_id = :b",
                Map.of("a", actorId, "b", targetId));
    }

    @Override
    public boolean blocked(long actorId, long targetId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM member_block WHERE actor_id = :a AND target_id = :b)",
                Map.of("a", actorId, "b", targetId), Boolean.class));
    }

    @Override
    public boolean blockedEitherWay(long memberA, long memberB) {
        Boolean blocked = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM member_block
                               WHERE (actor_id = :a AND target_id = :b) OR (actor_id = :b AND target_id = :a))
                """, Map.of("a", memberA, "b", memberB), Boolean.class);
        return Boolean.TRUE.equals(blocked);
    }

    @Override
    public Set<Long> relatedMemberIds(long memberId) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT target_id FROM member_block WHERE actor_id = :m
                UNION
                SELECT actor_id FROM member_block WHERE target_id = :m
                """, Map.of("m", memberId), Long.class));
    }

    @Override
    public List<BlockedMemberResult> blockedBy(long actorId, Long before, int limit) {
        var params = new MapSqlParameterSource("actor", actorId).addValue("limit", limit);
        String cursor = "";
        if (before != null) {
            cursor = " AND b.target_id < :before";
            params.addValue("before", before);
        }
        // 키셋 페이지: 앞 페이지에서 차단을 풀어도 다음 페이지가 밀리지 않는다.
        return jdbc.query("""
                SELECT b.target_id, m.nickname FROM member_block b JOIN member m ON m.id = b.target_id
                WHERE b.actor_id = :actor""" + cursor + " ORDER BY b.target_id DESC LIMIT :limit",
                params, (rs, n) -> new BlockedMemberResult(rs.getLong(1), rs.getString(2)));
    }
}
