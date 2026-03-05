package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.GetMediaQuery;
import me.yeonjae.ovlo.application.port.out.media.LoadMediaPort;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.media.model.MediaId;
import org.springframework.stereotype.Service;

@Service
public class MediaQueryService implements GetMediaQuery {

    private final LoadMediaPort loadMediaPort;

    public MediaQueryService(LoadMediaPort loadMediaPort) {
        this.loadMediaPort = loadMediaPort;
    }

    @Override
    public MediaResult getMedia(Long mediaId) {
        return loadMediaPort.findById(new MediaId(mediaId))
                .map(MediaResult::from)
                .orElseThrow(() -> new MediaException("미디어 파일을 찾을 수 없습니다: " + mediaId));
    }
}
