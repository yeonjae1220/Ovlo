package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.UploadMediaCommand;
import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.out.media.ImageConverterPort;
import me.yeonjae.ovlo.application.port.out.media.SaveMediaPort;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.domain.media.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MediaCommandServiceTest {

    @Mock SaveMediaPort saveMediaPort;
    @Mock StoragePort storagePort;
    @Mock ImageConverterPort imageConverterPort;

    @InjectMocks
    MediaCommandService service;

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("JPEG 파일을 업로드할 수 있다")
        void shouldUpload_jpegFile() {
            byte[] data = "jpeg-data".getBytes();
            UploadMediaCommand command = new UploadMediaCommand(1L, "photo.jpg", MediaType.IMAGE_JPEG, data);
            StoragePath storagePath = new StoragePath("uploads/photo.jpg", StorageType.LOCAL);
            MediaFile saved = MediaFile.restore(
                    new MediaId(1L), new MemberId(1L),
                    MediaType.IMAGE_JPEG, storagePath, "photo.jpg", data.length);

            given(storagePort.store(any(), eq(MediaType.IMAGE_JPEG), any())).willReturn(storagePath);
            given(saveMediaPort.save(any())).willReturn(saved);

            MediaResult result = service.upload(command);

            assertThat(result.mediaId()).isEqualTo(1L);
            assertThat(result.mediaType()).isEqualTo("IMAGE_JPEG");
            verify(imageConverterPort, never()).convertHeicToJpeg(any());
        }

        @Test
        @DisplayName("HEIC 파일 업로드 시 JPEG로 변환된 후 저장된다")
        void shouldConvert_heicToJpeg_beforeUpload() {
            byte[] heicData = "heic-data".getBytes();
            byte[] jpegData = "jpeg-converted".getBytes();
            UploadMediaCommand command = new UploadMediaCommand(1L, "photo.heic", MediaType.IMAGE_HEIC, heicData);
            StoragePath storagePath = new StoragePath("uploads/photo.jpg", StorageType.LOCAL);
            MediaFile saved = MediaFile.restore(
                    new MediaId(1L), new MemberId(1L),
                    MediaType.IMAGE_JPEG, storagePath, "photo.heic", jpegData.length);

            given(imageConverterPort.convertHeicToJpeg(heicData)).willReturn(jpegData);
            given(storagePort.store(eq(jpegData), eq(MediaType.IMAGE_JPEG), any())).willReturn(storagePath);
            given(saveMediaPort.save(any())).willReturn(saved);

            MediaResult result = service.upload(command);

            assertThat(result.mediaType()).isEqualTo("IMAGE_JPEG");
            verify(imageConverterPort).convertHeicToJpeg(heicData);
            verify(storagePort).store(eq(jpegData), eq(MediaType.IMAGE_JPEG), any());
        }
    }
}
