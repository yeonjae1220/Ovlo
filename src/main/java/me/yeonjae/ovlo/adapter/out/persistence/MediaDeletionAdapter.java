package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.media.MediaDeletionPort;
import me.yeonjae.ovlo.domain.media.model.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class MediaDeletionAdapter implements MediaDeletionPort {
    private final JdbcTemplate jdbc;
    public MediaDeletionAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override @Transactional
    public List<Pending> claim(int limit) {
        return jdbc.query("""
            WITH candidates AS (
              SELECT media_id FROM media_deletion_job WHERE next_attempt_at<=CURRENT_TIMESTAMP
              ORDER BY media_id LIMIT ? FOR UPDATE SKIP LOCKED
            ), claimed AS (
              UPDATE media_deletion_job j SET next_attempt_at=CURRENT_TIMESTAMP + interval '5 minutes', attempts=attempts+1
              FROM candidates c WHERE j.media_id=c.media_id RETURNING j.media_id
            )
            SELECT f.id,f.storage_path,f.storage_type FROM claimed c JOIN media_file f ON f.id=c.media_id
            """, (rs, row) -> new Pending(rs.getLong(1), new StoragePath(rs.getString(2), StorageType.valueOf(rs.getString(3)))), limit);
    }
    @Override @Transactional
    public void complete(Long mediaId) {
        jdbc.update("DELETE FROM media_file WHERE id=? AND EXISTS (SELECT 1 FROM media_deletion_job WHERE media_id=?)", mediaId, mediaId);
    }
}
