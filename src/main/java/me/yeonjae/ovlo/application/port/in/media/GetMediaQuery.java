package me.yeonjae.ovlo.application.port.in.media;

import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;
import me.yeonjae.ovlo.application.dto.result.MediaResult;

public interface GetMediaQuery {
    MediaResult getMedia(Long mediaId);
    MediaDownloadResult downloadMedia(Long mediaId);
}
