package me.yeonjae.ovlo.application.dto.result;

public record MediaDownloadResult(
        byte[] data,
        String mediaType,
        String originalFilename
) {}
