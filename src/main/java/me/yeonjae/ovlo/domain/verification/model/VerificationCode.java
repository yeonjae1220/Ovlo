package me.yeonjae.ovlo.domain.verification.model;

import java.util.regex.Pattern;

/**
 * 이메일 인증 6자리 숫자 코드 (선행 0 허용 → 문자열 보관).
 * 짧은 코드라 해시는 무의미; 보안은 TTL + 시도횟수 캡 + 발송 쿨다운으로 확보한다.
 */
public record VerificationCode(String value) {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    public VerificationCode {
        if (value == null || !SIX_DIGITS.matcher(value).matches()) {
            throw new IllegalArgumentException("인증 코드는 6자리 숫자여야 합니다");
        }
    }

    /** 상수시간 비교(타이밍 누수 완화). */
    public boolean matches(VerificationCode other) {
        if (other == null) return false;
        byte[] a = value.getBytes();
        byte[] b = other.value.getBytes();
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
