package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.mapper.MediaFileMapper;
import me.yeonjae.ovlo.adapter.out.persistence.repository.MediaFileJpaRepository;
import me.yeonjae.ovlo.application.port.out.media.LoadMediaPort;
import me.yeonjae.ovlo.application.port.out.media.SaveMediaPort;
import me.yeonjae.ovlo.domain.media.model.MediaFile;
import me.yeonjae.ovlo.domain.media.model.MediaId;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class MediaPersistenceAdapter implements LoadMediaPort, SaveMediaPort {

    private final MediaFileJpaRepository mediaFileJpaRepository;
    private final MediaFileMapper mediaFileMapper;

    public MediaPersistenceAdapter(MediaFileJpaRepository mediaFileJpaRepository, MediaFileMapper mediaFileMapper) {
        this.mediaFileJpaRepository = mediaFileJpaRepository;
        this.mediaFileMapper = mediaFileMapper;
    }

    @Override
    public Optional<MediaFile> findById(MediaId mediaId) {
        return mediaFileJpaRepository.findById(mediaId.value()).map(mediaFileMapper::toDomain);
    }

    @Override
    public MediaFile save(MediaFile mediaFile) {
        return mediaFileMapper.toDomain(mediaFileJpaRepository.save(mediaFileMapper.toJpaEntity(mediaFile)));
    }

    @Override
    public void delete(MediaFile mediaFile) {
        if (mediaFile.getId() != null) {
            mediaFileJpaRepository.deleteById(mediaFile.getId().value());
        }
    }
}
