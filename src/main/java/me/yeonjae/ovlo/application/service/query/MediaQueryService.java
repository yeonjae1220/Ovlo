package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.GetMediaQuery;
import me.yeonjae.ovlo.application.port.out.media.LoadMediaPort;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.media.model.MediaFile;
import me.yeonjae.ovlo.domain.media.model.MediaId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MediaQueryService implements GetMediaQuery {

    private final LoadMediaPort loadMediaPort;
    private final StoragePort storagePort;

    public MediaQueryService(LoadMediaPort loadMediaPort, StoragePort storagePort) {
        this.loadMediaPort = loadMediaPort;
        this.storagePort = storagePort;
    }

    @Override
    public MediaResult getMedia(Long mediaId) {
        return MediaResult.from(findMediaOrThrow(mediaId));
    }

    @Override
    public MediaDownloadResult downloadMedia(Long mediaId) {
        MediaFile mediaFile = findMediaOrThrow(mediaId);
        byte[] data = storagePort.retrieve(mediaFile.getStoragePath());
        return new MediaDownloadResult(data, mediaFile.getMediaType().name(), mediaFile.getOriginalFilename());
    }

    private MediaFile findMediaOrThrow(Long mediaId) {
        return loadMediaPort.findById(new MediaId(mediaId))
                .orElseThrow(() -> new MediaException("미디어 파일을 찾을 수 없습니다: " + mediaId));
    }
}
