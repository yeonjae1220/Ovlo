package me.yeonjae.ovlo.adapter.out.persistence;
import me.yeonjae.ovlo.application.port.out.chat.ChatMessagePolicyPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceChatPolicyAdapter implements ChatMessagePolicyPort {
    private final JdbcTemplate jdbc;
    public MarketplaceChatPolicyAdapter(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override public void requireMessageAllowed(long roomId) {
        long denied=jdbc.queryForObject("""
            SELECT count(*) FROM marketplace_chat c
            JOIN member buyer ON buyer.id=c.buyer_id JOIN member seller ON seller.id=c.seller_id
            WHERE c.room_id=? AND (buyer.status <> 'ACTIVE' OR seller.status <> 'ACTIVE' OR EXISTS (
                SELECT 1 FROM marketplace_block b WHERE (b.actor_id=c.buyer_id AND b.target_id=c.seller_id)
                    OR (b.actor_id=c.seller_id AND b.target_id=c.buyer_id)))
            """,Long.class,roomId);
        if(denied>0) throw new ChatException("Marketplace messaging is blocked",ChatException.ErrorType.FORBIDDEN);
    }
}
