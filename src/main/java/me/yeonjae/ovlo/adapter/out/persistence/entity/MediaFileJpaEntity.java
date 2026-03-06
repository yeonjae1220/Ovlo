package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.media.model.StorageType;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "media_file")
public class MediaFileJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "uploader_id", nullable = false) private Long uploaderId;
    @Column(name = "media_type", nullable = false) @Enumerated(EnumType.STRING) private MediaType mediaType;
    @Column(name = "storage_path", nullable = false) private String storagePath;
    @Column(name = "storage_type", nullable = false) @Enumerated(EnumType.STRING) private StorageType storageType;
    @Column(name = "original_filename", nullable = false) private String originalFilename;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public MediaFileJpaEntity() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUploaderId() { return uploaderId; } public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public MediaType getMediaType() { return mediaType; } public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public StorageType getStorageType() { return storageType; } public void setStorageType(StorageType storageType) { this.storageType = storageType; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public long getFileSize() { return fileSize; } public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}
