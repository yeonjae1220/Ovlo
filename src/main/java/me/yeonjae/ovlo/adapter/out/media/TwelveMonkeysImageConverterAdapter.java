package me.yeonjae.ovlo.adapter.out.media;

import me.yeonjae.ovlo.application.port.out.media.ImageConverterPort;
import me.yeonjae.ovlo.domain.media.exception.MediaException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class TwelveMonkeysImageConverterAdapter implements ImageConverterPort {

    @Override
    public byte[] convertHeicToJpeg(byte[] heicData) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(heicData);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new MediaException("HEIC 이미지를 읽을 수 없습니다");
            }
            boolean success = ImageIO.write(image, "JPEG", out);
            if (!success) {
                throw new MediaException("JPEG 변환에 실패했습니다");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new MediaException("HEIC → JPEG 변환 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
