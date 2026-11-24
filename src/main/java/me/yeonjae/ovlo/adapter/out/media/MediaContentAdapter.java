package me.yeonjae.ovlo.adapter.out.media;

import me.yeonjae.ovlo.application.port.out.media.MediaContentPort;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class MediaContentAdapter implements MediaContentPort {
    @Override public byte[] validate(byte[] data, MediaType type) {
        if (data == null || data.length == 0 || data.length > 20*1024*1024)
            throw new IllegalArgumentException("Use a nonempty file under 20 MB");
        if (type == MediaType.VIDEO_MP4) {
            if (data.length < 16 || !new String(data,4,4,StandardCharsets.US_ASCII).equals("ftyp")
                    || !java.util.Set.of("isom","iso2","mp41","mp42","avc1","M4V ").contains(new String(data,8,4,StandardCharsets.US_ASCII)))
                throw new IllegalArgumentException("The file is not a supported MP4");
            return data;
        }
        if (type != MediaType.IMAGE_JPEG && type != MediaType.IMAGE_PNG)
            throw new IllegalArgumentException("Use JPEG or PNG. HEIC and WebP uploads are not supported yet");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Cannot read this image");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = type == MediaType.IMAGE_PNG ? "png" : "jpeg";
                if (!reader.getFormatName().equalsIgnoreCase(format)) throw new IllegalArgumentException("Image type does not match its content");
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if (w<1 || h<1 || (long)w*h>40_000_000) throw new IllegalArgumentException("Image exceeds 40 megapixels");
                reader.read(0); // Validate decoding, preserving original orientation/color metadata.
                return data;
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw new IllegalArgumentException("Cannot read this image"); }
    }
}
