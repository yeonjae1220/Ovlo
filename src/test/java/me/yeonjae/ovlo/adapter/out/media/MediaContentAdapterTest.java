package me.yeonjae.ovlo.adapter.out.media;

import me.yeonjae.ovlo.domain.media.model.MediaType;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.assertj.core.api.Assertions.*;

class MediaContentAdapterTest {
    final MediaContentAdapter adapter = new MediaContentAdapter();
    @Test void realPngCanBeReadAndSpoofedTypesAreRejected() throws Exception {
        var image = new BufferedImage(3,2,BufferedImage.TYPE_INT_RGB);
        image.setRGB(1,1,0x229944);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        var result = ImageIO.read(new ByteArrayInputStream(adapter.validate(bytes.toByteArray(),MediaType.IMAGE_PNG)));
        assertThat(result.getRGB(1,1)).isEqualTo(image.getRGB(1,1));
        assertThatThrownBy(() -> adapter.validate(bytes.toByteArray(),MediaType.IMAGE_JPEG)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.validate("<html>not an image".getBytes(),MediaType.IMAGE_PNG)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void unavailableCodecsAndEmptyFilesHaveClearErrors() {
        assertThatThrownBy(() -> adapter.validate(new byte[20],MediaType.IMAGE_HEIC)).hasMessageContaining("JPEG or PNG");
        assertThatThrownBy(() -> adapter.validate(new byte[0],MediaType.IMAGE_JPEG)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.validate(new byte[20],MediaType.VIDEO_MP4)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void realJpegIsValidatedWithoutChangingOriginalBytes() throws Exception {
        var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, "jpeg", bytes)).isTrue();
        byte[] original = bytes.toByteArray();
        assertThat(adapter.validate(original, MediaType.IMAGE_JPEG)).isEqualTo(original);
        assertThat(ImageIO.read(new ByteArrayInputStream(original)).getWidth()).isEqualTo(3);
    }
}
