package me.yeonjae.ovlo.domain.media.model;

public enum MediaType {
    IMAGE_JPEG,
    IMAGE_PNG,
    IMAGE_HEIC,
    IMAGE_WEBP,
    VIDEO_MP4;

    public boolean isHeic() {
        return this == IMAGE_HEIC;
    }

    public boolean isImage() {
        return name().startsWith("IMAGE_");
    }

    public boolean isVideo() {
        return name().startsWith("VIDEO_");
    }
}
