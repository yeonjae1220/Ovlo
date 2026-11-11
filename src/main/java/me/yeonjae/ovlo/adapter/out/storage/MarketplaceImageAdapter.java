package me.yeonjae.ovlo.adapter.out.storage;

import me.yeonjae.ovlo.application.port.out.marketplace.MarketplaceImagePort;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

@Component
public class MarketplaceImageAdapter implements MarketplaceImagePort {
    @Override public byte[] sanitize(byte[] data) {
        if(data==null || data.length==0 || data.length>8*1024*1024) throw new IllegalArgumentException("Photo must be under 8 MB");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IllegalArgumentException("Use a JPEG or PNG photo");
            var reader=readers.next();
            try {
                reader.setInput(input,true,true);
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG")) throw new IllegalArgumentException("Use a JPEG or PNG photo");
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<=0 || height<=0 || (long)width*height>40000000) throw new IllegalArgumentException("Photo exceeds 40 megapixels");
                var param=reader.getDefaultReadParam();
                int sample=Math.max(1,Math.max(width,height)/1600); param.setSourceSubsampling(sample,sample,0,0);
                BufferedImage source=reader.read(0,param);
                double ratio=Math.min(1,1600.0/Math.max(source.getWidth(),source.getHeight()));
                var output=new BufferedImage(Math.max(1,(int)(source.getWidth()*ratio)),Math.max(1,(int)(source.getHeight()*ratio)),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=output.createGraphics();
                try { g.setColor(Color.WHITE);g.fillRect(0,0,output.getWidth(),output.getHeight());g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,output.getWidth(),output.getHeight(),null); } finally {g.dispose();}
                var bytes=new ByteArrayOutputStream(); ImageIO.write(output,"jpeg",bytes); return bytes.toByteArray();
            } finally { reader.dispose(); }
        } catch(IOException e) { throw new IllegalArgumentException("Cannot read this photo"); }
    }
}
