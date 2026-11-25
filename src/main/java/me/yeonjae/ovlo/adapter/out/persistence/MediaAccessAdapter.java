package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.media.MediaAccessPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MediaAccessAdapter implements MediaAccessPort {
    private final JdbcTemplate jdbc;
    public MediaAccessAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public boolean isOwnedUpload(Long mediaId, Long requesterId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM media_file f JOIN member m ON m.id=f.uploader_id WHERE f.id=? AND m.id=? AND m.status='ACTIVE')",Boolean.class,mediaId,requesterId));
    }

    @Override public boolean canReadAttachment(Long mediaId, Long requesterId, Long officeId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM media_file f JOIN member owner ON owner.id=f.uploader_id AND owner.status='ACTIVE'
              JOIN member viewer ON viewer.id=? AND viewer.status='ACTIVE'
              WHERE f.id=? AND EXISTS(SELECT 1 FROM office_staff s WHERE s.office_id=? AND s.member_id=owner.id)
              AND NOT EXISTS(SELECT 1 FROM member_block b
                WHERE (b.actor_id=viewer.id AND b.target_id=owner.id) OR (b.actor_id=owner.id AND b.target_id=viewer.id)))
            """,Boolean.class,requesterId,mediaId,officeId));
    }

    @Override
    public boolean canRead(Long mediaId, Long requesterId) {
        if (mediaId == null || requesterId == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
              SELECT 1 FROM media_file f
              JOIN member owner ON owner.id=f.uploader_id AND owner.status='ACTIVE'
              JOIN member viewer ON viewer.id=? AND viewer.status='ACTIVE'
              WHERE f.id=?
                AND (f.uploader_id=viewer.id OR owner.profile_image_media_id=CAST(f.id AS varchar))
                AND NOT EXISTS (SELECT 1 FROM member_block b
                  WHERE (b.actor_id=viewer.id AND b.target_id=owner.id)
                     OR (b.actor_id=owner.id AND b.target_id=viewer.id))
            )
            """, Boolean.class, requesterId, mediaId));
    }

    @Override
    public boolean canUseAsProfileImage(Long mediaId, Long requesterId) {
        if (mediaId == null || requesterId == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (SELECT 1 FROM media_file f JOIN member m ON m.id=f.uploader_id
              WHERE f.id=? AND f.uploader_id=? AND m.status='ACTIVE'
                AND f.media_type IN ('IMAGE_JPEG','IMAGE_PNG','IMAGE_WEBP'))
            """, Boolean.class, mediaId, requesterId));
    }
}
