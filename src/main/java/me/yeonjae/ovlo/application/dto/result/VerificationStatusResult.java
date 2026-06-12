package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/** 본인 인증 현황 (뱃지 표시용). */
public record VerificationStatusResult(
        String trustLevel,
        List<VerifiedUniversity> verifiedUniversities
) {
    public record VerifiedUniversity(
            Long universityId,
            String verifiedEmail,
            String verifiedAt
    ) {}
}
