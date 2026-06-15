package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/**
 * 관리자 인증 관리 화면용 회원 인증 현황.
 *
 * @param memberId    대상 회원
 * @param trustLevel  보유 자격에서 파생된 신뢰 등급
 * @param credentials 보유 자격 목록(만료 포함)
 */
public record AdminVerificationView(
        Long memberId,
        String trustLevel,
        List<Credential> credentials
) {
    public record Credential(
            Long credentialId,
            Long universityId,
            String universityName,
            String type,
            String status,
            String verifiedEmail,
            String verifiedBy,
            String note,
            String verifiedAt,
            String revokedBy,
            String revokedAt
    ) {}
}
