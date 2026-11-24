package me.yeonjae.ovlo.application.port.in.media;

import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;

public interface GetNoticeAttachmentQuery {
    MediaDownloadResult download(long officeId, long noticeId, long mediaId, long requesterId, boolean staff);
}
