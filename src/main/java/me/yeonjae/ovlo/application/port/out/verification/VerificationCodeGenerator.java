package me.yeonjae.ovlo.application.port.out.verification;

import me.yeonjae.ovlo.domain.verification.model.VerificationCode;

/** 6자리 인증 코드 생성 (구현은 SecureRandom 기반). 테스트에서 고정 코드로 대체 가능. */
public interface VerificationCodeGenerator {
    VerificationCode generate();
}
