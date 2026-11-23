package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoLinkResult;
import me.yeonjae.ovlo.application.port.in.university.GetUniversityVideoLinksQuery;
import me.yeonjae.ovlo.application.port.out.university.VideoLinkStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UniversityVideoLinkQueryService implements GetUniversityVideoLinksQuery {

    private final VideoLinkStore store;

    public UniversityVideoLinkQueryService(VideoLinkStore store) {
        this.store = store;
    }

    @Override
    public PageResult<VideoLinkResult> getVideoLinks(long globalUnivId, int page, int size) {
        return PageResult.of(
                store.findByGlobalUniversity(globalUnivId, size, page * size),
                store.countByGlobalUniversity(globalUnivId),
                page, size);
    }
}
