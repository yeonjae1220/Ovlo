package me.yeonjae.ovlo.adapter.out.persistence;

import java.time.Duration;
import java.util.UUID;
import me.yeonjae.ovlo.application.port.out.marketplace.TranslationLeasePort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplaceTranslationLeaseAdapter implements TranslationLeasePort {
    private final NamedParameterJdbcTemplate jdbc;
    public MarketplaceTranslationLeaseAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    private MapSqlParameterSource parameters(Key key, UUID owner) {
        return new MapSqlParameterSource("id",key.listingId()).addValue("version",key.version())
                .addValue("language",key.language()).addValue("owner",owner);
    }
    @Override public boolean acquire(Key key, UUID owner, Duration lifetime) {
        return jdbc.update("""
                INSERT INTO marketplace_translation_lease(listing_id,version,language,owner,expires_at)
                VALUES(:id,:version,:language,:owner,clock_timestamp()+:seconds*interval '1 second')
                ON CONFLICT(listing_id,version,language) DO UPDATE
                SET owner=EXCLUDED.owner, expires_at=EXCLUDED.expires_at
                WHERE marketplace_translation_lease.expires_at <= clock_timestamp()
                """,parameters(key,owner).addValue("seconds",lifetime.toSeconds()))==1;
    }
    @Override public boolean renew(Key key, UUID owner, Duration lifetime) {
        return jdbc.update("""
                UPDATE marketplace_translation_lease SET expires_at=clock_timestamp()+:seconds*interval '1 second'
                WHERE listing_id=:id AND version=:version AND language=:language AND owner=:owner
                AND expires_at>clock_timestamp()
                """,parameters(key,owner).addValue("seconds",lifetime.toSeconds()))==1;
    }
    @Override public boolean lockOwned(Key key, UUID owner) {
        return !jdbc.queryForList("""
                SELECT owner FROM marketplace_translation_lease
                WHERE listing_id=:id AND version=:version AND language=:language AND owner=:owner
                AND expires_at>clock_timestamp() FOR UPDATE
                """,parameters(key,owner),UUID.class).isEmpty();
    }
    @Override public void release(Key key, UUID owner) {
        jdbc.update("DELETE FROM marketplace_translation_lease WHERE listing_id=:id AND version=:version AND language=:language AND owner=:owner",parameters(key,owner));
    }
}
