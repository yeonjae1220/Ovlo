package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.port.in.media.GetNoticeAttachmentQuery;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.port.out.media.*;
import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;
import me.yeonjae.ovlo.application.service.office.*;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.media.model.MediaId;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class NoticeAttachmentQueryService implements GetNoticeAttachmentQuery {
    private final GetNoticeQuery notices;
    private final StudentOfficeAccessPolicy students;
    private final CurrentOfficeStaffAccess staffAccess;
    private final LoadMediaPort media;
    private final MediaAccessPort access;
    private final StoragePort storage;
    public NoticeAttachmentQueryService(GetNoticeQuery notices, StudentOfficeAccessPolicy students,
            CurrentOfficeStaffAccess staffAccess, LoadMediaPort media, MediaAccessPort access, StoragePort storage) {
        this.notices=notices; this.students=students; this.staffAccess=staffAccess;
        this.media=media; this.access=access; this.storage=storage;
    }
    @Override public MediaDownloadResult download(long officeId,long noticeId,long mediaId,long requester,boolean staff) {
        java.util.List<Long> attachments;
        if (staff) {
            staffAccess.check(officeId,requester);
            attachments=notices.findDetail(officeId,noticeId).orElseThrow(this::missing).attachmentMediaIds();
        } else {
            students.assertMember(requester,officeId);
            attachments=notices.findStudentDetail(officeId,noticeId,NoticeLang.KO).orElseThrow(this::missing).attachmentMediaIds();
        }
        if (!attachments.contains(mediaId) || !access.canReadAttachment(mediaId,requester,officeId)) throw missing();
        var file=media.findById(new MediaId(mediaId)).orElseThrow(this::missing);
        return new MediaDownloadResult(storage.retrieve(file.getStoragePath()),file.getMediaType().name(),file.getOriginalFilename());
    }
    private MediaException missing() { return new MediaException("첨부 파일을 찾을 수 없습니다"); }
}
