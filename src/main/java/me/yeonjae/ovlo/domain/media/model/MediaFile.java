package me.yeonjae.ovlo.domain.media.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Objects;

public class MediaFile {

    private MediaId id;
    private MemberId uploaderId;
    private MediaType mediaType;
    private StoragePath storagePath;
    private String originalFilename;
    private long fileSize;

    private MediaFile() {}

    public static MediaFile create(
            MemberId uploaderId,
            MediaType mediaType,
            StoragePath storagePath,
            String originalFilename,
            long fileSize) {

        Objects.requireNonNull(uploaderId, "업로더 ID는 필수입니다");
        Objects.requireNonNull(mediaType, "미디어 타입은 필수입니다");
        Objects.requireNonNull(storagePath, "저장 경로는 필수입니다");
        Objects.requireNonNull(originalFilename, "원본 파일명은 필수입니다");
        if (originalFilename.isBlank()) {
            throw new IllegalArgumentException("원본 파일명은 빈 값일 수 없습니다");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("파일 크기는 0보다 커야 합니다: " + fileSize);
        }

        MediaFile mediaFile = new MediaFile();
        mediaFile.uploaderId = uploaderId;
        mediaFile.mediaType = mediaType;
        mediaFile.storagePath = storagePath;
        mediaFile.originalFilename = originalFilename;
        mediaFile.fileSize = fileSize;
        return mediaFile;
    }

    /** persistence 계층 전용: DB에서 모든 필드를 복원할 때 사용 */
    public static MediaFile restore(
            MediaId id,
            MemberId uploaderId,
            MediaType mediaType,
            StoragePath storagePath,
            String originalFilename,
            long fileSize) {

        MediaFile mediaFile = new MediaFile();
        mediaFile.id = id;
        mediaFile.uploaderId = uploaderId;
        mediaFile.mediaType = mediaType;
        mediaFile.storagePath = storagePath;
        mediaFile.originalFilename = originalFilename;
        mediaFile.fileSize = fileSize;
        return mediaFile;
    }

    public MediaId getId() { return id; }
    public MemberId getUploaderId() { return uploaderId; }
    public MediaType getMediaType() { return mediaType; }
    public StoragePath getStoragePath() { return storagePath; }
    public String getOriginalFilename() { return originalFilename; }
    public long getFileSize() { return fileSize; }
}
