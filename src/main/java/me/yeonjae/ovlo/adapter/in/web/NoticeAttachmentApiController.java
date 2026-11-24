package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.media.GetNoticeAttachmentQuery;
import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;

@RestController
public class NoticeAttachmentApiController {
    private final GetNoticeAttachmentQuery query;
    public NoticeAttachmentApiController(GetNoticeAttachmentQuery query) { this.query=query; }
    @GetMapping("/api/v1/office/{officeId}/notices/{noticeId}/attachments/{mediaId}")
    public ResponseEntity<byte[]> student(@PathVariable long officeId,@PathVariable long noticeId,@PathVariable long mediaId,@AuthenticationPrincipal Long memberId) {
        return response(query.download(officeId,noticeId,mediaId,memberId,false));
    }
    @GetMapping("/api/v1/office-admin/{officeId}/notices/{noticeId}/attachments/{mediaId}")
    public ResponseEntity<byte[]> staff(@PathVariable long officeId,@PathVariable long noticeId,@PathVariable long mediaId,@AuthenticationPrincipal Long memberId) {
        return response(query.download(officeId,noticeId,mediaId,memberId,true));
    }
    private ResponseEntity<byte[]> response(MediaDownloadResult file) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.originalFilename(),StandardCharsets.UTF_8).build().toString())
                .body(file.data());
    }
}
