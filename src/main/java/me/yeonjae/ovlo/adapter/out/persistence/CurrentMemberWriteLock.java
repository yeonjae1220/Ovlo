package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.domain.member.exception.MemberException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Private writers acquire the member row before child rows, matching withdrawal. */
@Component
public class CurrentMemberWriteLock {
    private final JdbcTemplate jdbc;
    public CurrentMemberWriteLock(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void requireActive(long id) {
        var states = jdbc.queryForList("SELECT status FROM member WHERE id=? FOR UPDATE", String.class, id);
        if (states.size() != 1 || !"ACTIVE".equals(states.getFirst())) {
            throw new MemberException("사용할 수 없는 회원입니다", MemberException.ErrorType.NOT_FOUND);
        }
    }
}
