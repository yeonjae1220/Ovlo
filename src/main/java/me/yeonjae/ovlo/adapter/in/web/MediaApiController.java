package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaDownloadResult;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.GetMediaQuery;
import me.yeonjae.ovlo.application.port.in.media.UploadMediaUseCase;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Tag(name = "Media", description = "미디어 파일 API")
@RestController
@RequestMapping("/api/v1/media")
public class MediaApiController {

    private final UploadMediaUseCase uploadMediaUseCase;
    private final GetMediaQuery getMediaQuery;

    public MediaApiController(UploadMediaUseCase uploadMediaUseCase, GetMediaQuery getMediaQuery) {
        this.uploadMediaUseCase = uploadMediaUseCase;
        this.getMediaQuery = getMediaQuery;
    }

    @Operation(summary = "미디어 파일 메타데이터 조회")
    @GetMapping("/{id}")
    public ResponseEntity<MediaResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getMediaQuery.getMedia(id));
    }

    @Operation(summary = "미디어 파일 바이너리 다운로드")
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        MediaDownloadResult download = getMediaQuery.downloadMedia(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveContentType(download.mediaType()));
        headers.setContentDisposition(
                ContentDisposition.inline()
                        .filename(download.originalFilename(), StandardCharsets.UTF_8)
                        .build()
        );
        return ResponseEntity.ok().headers(headers).body(download.data());
    }

    private org.springframework.http.MediaType resolveContentType(String mediaTypeName) {
        return switch (mediaTypeName) {
            case "IMAGE_JPEG" -> org.springframework.http.MediaType.IMAGE_JPEG;
            case "IMAGE_PNG"  -> org.springframework.http.MediaType.IMAGE_PNG;
            case "IMAGE_WEBP" -> org.springframework.http.MediaType.parseMediaType("image/webp");
            case "VIDEO_MP4"  -> org.springframework.http.MediaType.parseMediaType("video/mp4");
            default           -> org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    @Operation(summary = "미디어 파일 업로드")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") String mediaType,
            @AuthenticationPrincipal Long memberId
    ) {
        MediaType type = MediaType.valueOf(mediaType);

        String originalFilename = file.getOriginalFilename();
        String safeFilename = (originalFilename != null)
                ? Paths.get(originalFilename).getFileName().toString()
                : "upload";

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new MediaException("파일을 읽는 중 오류가 발생했습니다: " + safeFilename);
        }

        MediaResult result = uploadMediaUseCase.upload(
                new UploadMediaCommand(
                        memberId,
                        safeFilename,
                        type,
                        data
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
