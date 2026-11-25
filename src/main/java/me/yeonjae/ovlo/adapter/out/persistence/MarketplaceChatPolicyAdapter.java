package me.yeonjae.ovlo.adapter.out.persistence;
import me.yeonjae.ovlo.application.port.out.chat.ChatMessagePolicyPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 메시지를 보내도 되는 방인지 본다.
 * 중고거래 방: 양쪽이 활동 중이고 서로 차단하지 않았을 때만. DM: 두 참여자가 서로 차단하지 않았을 때만.
 * 단체방(상담방 포함)은 차단과 무관하다 — 신고로 다룬다.
 */
@Component
public class MarketplaceChatPolicyAdapter implements ChatMessagePolicyPort {
    private final JdbcTemplate jdbc;
    public MarketplaceChatPolicyAdapter(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override public void requireMessageAllowed(long roomId) {
        long denied=jdbc.queryForObject("""
            SELECT count(*) FROM marketplace_chat c
            JOIN member buyer ON buyer.id=c.buyer_id JOIN member seller ON seller.id=c.seller_id
            WHERE c.room_id=? AND (buyer.status <> 'ACTIVE' OR seller.status <> 'ACTIVE' OR EXISTS (
                SELECT 1 FROM member_block b WHERE (b.actor_id=c.buyer_id AND b.target_id=c.seller_id)
                    OR (b.actor_id=c.seller_id AND b.target_id=c.buyer_id)))
            """,Long.class,roomId);
        if(denied>0) throw new ChatException("Marketplace messaging is blocked",ChatException.ErrorType.FORBIDDEN);
        long dmBlocked=jdbc.queryForObject("""
            SELECT count(*) FROM chat_room r
            JOIN chat_room_participant a ON a.chat_room_id=r.id
            JOIN chat_room_participant b ON b.chat_room_id=r.id AND b.member_id<>a.member_id
            WHERE r.id=? AND r.type='DM'
              AND EXISTS (SELECT 1 FROM member_block x WHERE x.actor_id=a.member_id AND x.target_id=b.member_id)
            """,Long.class,roomId);
        if(dmBlocked>0) throw new ChatException("상대와 메시지를 주고받을 수 없습니다",ChatException.ErrorType.FORBIDDEN);
    }
}
