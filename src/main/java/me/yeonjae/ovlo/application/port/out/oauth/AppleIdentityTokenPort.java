package me.yeonjae.ovlo.application.port.out.oauth;

import me.yeonjae.ovlo.application.dto.result.AppleIdentity;

public interface AppleIdentityTokenPort {
    /** 서명·iss·aud·exp 를 검증한다. 검증에 실패하면 AuthException. */
    AppleIdentity verify(String identityToken);
}
