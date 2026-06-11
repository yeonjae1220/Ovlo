package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/**
 * 학교 이메일 도메인 → 대학 역조회 결과.
 *
 * <ul>
 *   <li>{@code MATCHED}      : 정확히 한 대학과 매칭 (검증 성공 후보)</li>
 *   <li>{@code AMBIGUOUS}    : 동일 도메인을 가진 대학이 2곳 이상 (본교로 disambiguate 필요)</li>
 *   <li>{@code NOT_FOUND}    : 카탈로그에 없는 도메인 (학교 메일 아님/미등록)</li>
 *   <li>{@code PUBLIC_PROVIDER} : gmail 등 공개 메일 (학교 메일 아님)</li>
 * </ul>
 */
public record UniversityEmailResolution(
        Status status,
        String domain,
        List<UniversityResult> candidates
) {
    public enum Status { MATCHED, AMBIGUOUS, NOT_FOUND, PUBLIC_PROVIDER }

    public static UniversityEmailResolution matched(String domain, UniversityResult university) {
        return new UniversityEmailResolution(Status.MATCHED, domain, List.of(university));
    }

    public static UniversityEmailResolution ambiguous(String domain, List<UniversityResult> candidates) {
        return new UniversityEmailResolution(Status.AMBIGUOUS, domain, List.copyOf(candidates));
    }

    public static UniversityEmailResolution notFound(String domain) {
        return new UniversityEmailResolution(Status.NOT_FOUND, domain, List.of());
    }

    public static UniversityEmailResolution publicProvider(String domain) {
        return new UniversityEmailResolution(Status.PUBLIC_PROVIDER, domain, List.of());
    }

    public boolean isVerifiable() {
        return status == Status.MATCHED || status == Status.AMBIGUOUS;
    }

    /** 후보 중 주어진 대학 id가 포함되는지 (프로필 본교로 disambiguate). */
    public boolean matchesUniversity(Long universityId) {
        return candidates.stream().anyMatch(c -> c.id().equals(universityId));
    }
}
