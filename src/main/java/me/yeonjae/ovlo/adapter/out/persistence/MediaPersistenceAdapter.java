package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.media.LoadMediaPort;
import me.yeonjae.ovlo.application.port.out.media.SaveMediaPort;
import me.yeonjae.ovlo.domain.media.model.MediaFile;
import me.yeonjae.ovlo.domain.media.model.MediaId;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Media JPA 구현 예정 (stub).
 * LoadMediaPort / SaveMediaPort 구현.
 */
@Component
public class MediaPersistenceAdapter implements LoadMediaPort, SaveMediaPort {

    @Override
    public Optional<MediaFile> findById(MediaId mediaId) {
        throw new UnsupportedOperationException("Media JPA 구현 예정");
    }

    @Override
    public MediaFile save(MediaFile mediaFile) {
        throw new UnsupportedOperationException("Media JPA 구현 예정");
    }

    @Override
    public void delete(MediaFile mediaFile) {
        throw new UnsupportedOperationException("Media JPA 구현 예정");
    }
}
