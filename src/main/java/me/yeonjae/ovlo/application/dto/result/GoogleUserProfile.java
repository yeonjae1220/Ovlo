package me.yeonjae.ovlo.application.dto.result;

public record GoogleUserProfile(
        String email,
        String name,
        String picture,
        String googleId
) {}
