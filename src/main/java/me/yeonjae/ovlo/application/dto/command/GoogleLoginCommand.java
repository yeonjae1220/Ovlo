package me.yeonjae.ovlo.application.dto.command;

public record GoogleLoginCommand(String code, String redirectUri) {}
