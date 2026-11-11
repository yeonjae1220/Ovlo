package me.yeonjae.ovlo.application.port.out.marketplace;
public interface MarketplaceImagePort {
    /** Decode and re-encode a bounded raster image, stripping metadata. Returns JPEG bytes. */
    byte[] sanitize(byte[] input);
}
