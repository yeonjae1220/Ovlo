package me.yeonjae.ovlo.application.port.out.auth;

import me.yeonjae.ovlo.application.dto.result.MemberCredentials;

import java.util.Optional;

public interface LoadMemberCredentialsPort {
    Optional<MemberCredentials> findByEmail(String email);
}
