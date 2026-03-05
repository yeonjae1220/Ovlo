package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MediaResult;
import me.yeonjae.ovlo.application.port.out.media.LoadMediaPort;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import me.yeonjae.ovlo.domain.media.model.*;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MediaQueryServiceTest {

    @Mock LoadMediaPort loadMediaPort;

    @InjectMocks
    MediaQueryService service;

    @Nested
    @DisplayName("getMedia()")
    class GetMedia {

        @Test
        @DisplayName("미디어 파일을 조회할 수 있다")
        void shouldGetMedia() {
            StoragePath storagePath = new StoragePath("uploads/test.jpg", StorageType.LOCAL);
            MediaFile mediaFile = MediaFile.restore(
                    new MediaId(1L), new MemberId(1L),
                    MediaType.IMAGE_JPEG, storagePath, "test.jpg", 1024L);
            given(loadMediaPort.findById(any())).willReturn(Optional.of(mediaFile));

            MediaResult result = service.getMedia(1L);

            assertThat(result.mediaId()).isEqualTo(1L);
            assertThat(result.originalFilename()).isEqualTo("test.jpg");
            assertThat(result.mediaType()).isEqualTo("IMAGE_JPEG");
        }

        @Test
        @DisplayName("존재하지 않는 미디어 파일 조회 시 예외가 발생한다")
        void shouldThrow_whenMediaNotFound() {
            given(loadMediaPort.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMedia(999L))
                    .isInstanceOf(MediaException.class)
                    .hasMessageContaining("미디어 파일을 찾을 수 없습니다");
        }
    }
}
