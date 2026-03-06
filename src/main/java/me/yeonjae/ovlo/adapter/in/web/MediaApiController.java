package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.in.media.GetMediaQuery;
import me.yeonjae.ovlo.application.port.in.media.UploadMediaUseCase;
import me.yeonjae.ovlo.domain.media.model.MediaType;
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

    @Operation(summary = "미디어 파일 조회")
    @GetMapping("/{id}")
    public ResponseEntity<MediaResult> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getMediaQuery.getMedia(id));
    }

    @Operation(summary = "미디어 파일 업로드")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResult> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") String mediaType,
            @AuthenticationPrincipal Long memberId
    ) throws IOException {
        MediaType type = MediaType.valueOf(mediaType);

        MediaResult result = uploadMediaUseCase.upload(
                new UploadMediaCommand(
                        memberId,
                        file.getOriginalFilename(),
                        type,
                        file.getBytes()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
