package me.yeonjae.ovlo.domain.media.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaFileTest {

    private final MemberId uploaderId = new MemberId(1L);
    private final StoragePath storagePath = new StoragePath("uploads/test.jpg", StorageType.LOCAL);

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("JPEG 파일로 MediaFile을 생성할 수 있다")
        void shouldCreate_jpegMediaFile() {
            MediaFile mediaFile = MediaFile.create(uploaderId, MediaType.IMAGE_JPEG, storagePath, "test.jpg", 1024L);

            assertThat(mediaFile.getId()).isNull();
            assertThat(mediaFile.getUploaderId()).isEqualTo(uploaderId);
            assertThat(mediaFile.getMediaType()).isEqualTo(MediaType.IMAGE_JPEG);
            assertThat(mediaFile.getStoragePath()).isEqualTo(storagePath);
            assertThat(mediaFile.getOriginalFilename()).isEqualTo("test.jpg");
            assertThat(mediaFile.getFileSize()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("HEIC 파일로 MediaFile을 생성할 수 있다")
        void shouldCreate_heicMediaFile() {
            MediaFile mediaFile = MediaFile.create(uploaderId, MediaType.IMAGE_HEIC, storagePath, "photo.heic", 2048L);

            assertThat(mediaFile.getMediaType()).isEqualTo(MediaType.IMAGE_HEIC);
            assertThat(mediaFile.getMediaType().isHeic()).isTrue();
        }

        @Test
        @DisplayName("uploaderId가 null이면 예외가 발생한다")
        void shouldThrow_whenUploaderIdIsNull() {
            assertThatThrownBy(() ->
                    MediaFile.create(null, MediaType.IMAGE_JPEG, storagePath, "test.jpg", 1024L))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("업로더 ID는 필수입니다");
        }

        @Test
        @DisplayName("originalFilename이 빈 값이면 예외가 발생한다")
        void shouldThrow_whenOriginalFilenameIsBlank() {
            assertThatThrownBy(() ->
                    MediaFile.create(uploaderId, MediaType.IMAGE_JPEG, storagePath, "  ", 1024L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("원본 파일명은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("fileSize가 0 이하이면 예외가 발생한다")
        void shouldThrow_whenFileSizeIsZeroOrNegative() {
            assertThatThrownBy(() ->
                    MediaFile.create(uploaderId, MediaType.IMAGE_JPEG, storagePath, "test.jpg", 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일 크기는 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("DB에서 MediaFile을 복원할 수 있다")
        void shouldRestore_mediaFile() {
            MediaId id = new MediaId(10L);

            MediaFile mediaFile = MediaFile.restore(id, uploaderId, MediaType.IMAGE_JPEG, storagePath, "test.jpg", 1024L);

            assertThat(mediaFile.getId()).isEqualTo(id);
            assertThat(mediaFile.getUploaderId()).isEqualTo(uploaderId);
            assertThat(mediaFile.getMediaType()).isEqualTo(MediaType.IMAGE_JPEG);
        }
    }
}
