package me.yeonjae.ovlo.adapter.out.verification;

import me.yeonjae.ovlo.application.port.out.verification.VerificationCodeGenerator;
import me.yeonjae.ovlo.domain.verification.model.VerificationCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** SecureRandom 기반 6자리 인증 코드 생성기 (선행 0 허용). */
@Component
public class SecureRandomCodeGenerator implements VerificationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public VerificationCode generate() {
        int n = RANDOM.nextInt(1_000_000); // 0 ~ 999999
        return new VerificationCode(String.format("%06d", n));
    }
}
