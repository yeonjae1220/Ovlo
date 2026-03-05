package me.yeonjae.ovlo.application.port.out.auth;

public interface PasswordHasherPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
