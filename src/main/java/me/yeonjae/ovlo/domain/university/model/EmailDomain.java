package me.yeonjae.ovlo.domain.university.model;

/**
 * 학교 이메일에서 추출·정규화한 도메인 (학생 인증 검증용).
 * 정규화 규칙: 소문자화, 공백/트레일링 닷 제거. 로컬파트의 +alias·점은 도메인에 영향 없음.
 * allowlist(global_universities.domain)와의 정확 매칭을 위해 임의 서브도메인 확장은 하지 않는다.
 */
public record EmailDomain(String value) {

    public EmailDomain {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일 도메인은 비어 있을 수 없습니다");
        }
    }

    /** 이메일 주소에서 도메인을 추출·정규화한다. */
    public static EmailDomain fromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }
        String normalized = email.trim().toLowerCase();
        int at = normalized.lastIndexOf('@');
        if (at < 1 || at == normalized.length() - 1) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다: " + email);
        }
        String domain = normalized.substring(at + 1);
        if (domain.endsWith(".")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (domain.isBlank() || !domain.contains(".") || domain.startsWith(".")) {
            throw new IllegalArgumentException("올바른 이메일 도메인이 아닙니다: " + email);
        }
        return new EmailDomain(domain);
    }
}
