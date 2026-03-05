package me.yeonjae.ovlo.application.port.out.media;

public interface ImageConverterPort {
    byte[] convertHeicToJpeg(byte[] heicData);
}
