package me.yeonjae.ovlo.application.dto.command;
public record UniversityRegistrationCommand(String nickname, String name, String email, String password,
        Long universityId, String department, String message) {}
