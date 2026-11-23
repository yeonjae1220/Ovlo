package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.VideoLinkRefreshResult;

public interface RefreshVideoLinksUseCase {

    /** 확인한 지 가장 오래된 링크부터 원본에 다시 물어 제목·채널명을 갱신하고 사라진 영상은 숨긴다. */
    VideoLinkRefreshResult refreshDue();
}
