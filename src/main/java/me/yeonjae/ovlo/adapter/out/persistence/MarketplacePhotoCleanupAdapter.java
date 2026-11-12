package me.yeonjae.ovlo.adapter.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import me.yeonjae.ovlo.application.port.out.marketplace.*;
import me.yeonjae.ovlo.domain.media.model.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketplacePhotoCleanupAdapter implements UnusedMarketplacePhotoStore {
    private final NamedParameterJdbcTemplate jdbc;
    public MarketplacePhotoCleanupAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public List<MarketplaceStore.Photo> claim(Instant cutoff,Instant retryAfter,int limit) {
        var photos=jdbc.query("""
                SELECT p.* FROM marketplace_photo p
                WHERE (p.deletion_pending OR p.orphaned_at<=:cutoff)
                  AND (p.cleanup_retry_at IS NULL OR p.cleanup_retry_at<=CURRENT_TIMESTAMP)
                  AND NOT EXISTS (SELECT 1 FROM marketplace_listing_photo lp WHERE lp.photo_id=p.id)
                ORDER BY p.orphaned_at,p.id LIMIT :limit FOR UPDATE OF p SKIP LOCKED
                """,Map.of("cutoff",Timestamp.from(cutoff),"limit",limit),
                (r,n)->new MarketplaceStore.Photo(r.getObject("id",UUID.class),r.getLong("owner_id"),
                        new StoragePath(r.getString("storage_path"),StorageType.valueOf(r.getString("storage_type")))));
        if(!photos.isEmpty()) jdbc.update("""
                UPDATE marketplace_photo SET deletion_pending=TRUE,cleanup_retry_at=:retry
                WHERE id IN (:ids)
                """,Map.of("retry",Timestamp.from(retryAfter),"ids",photos.stream().map(MarketplaceStore.Photo::id).toList()));
        return photos;
    }
    @Override public void complete(UUID id) {
        jdbc.update("""
                DELETE FROM marketplace_photo p WHERE p.id=:id AND p.deletion_pending
                AND NOT EXISTS (SELECT 1 FROM marketplace_listing_photo lp WHERE lp.photo_id=p.id)
                """,Map.of("id",id));
    }
}
