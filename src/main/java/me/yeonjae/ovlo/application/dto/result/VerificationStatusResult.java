package me.yeonjae.ovlo.application.dto.result;

import java.util.List;

/**
 * 본인 인증 현황 (뱃지 표시용).
 *
 * <p>{@code verifiedUniversities}에는 <b>활성</b>({@code VerificationCredential.isActive()}) 자격만 담는다.
 * UNIVERSITY 범위 게시판의 소속 게이트({@code PostCommandService})가 이 목록을 근거로 쓰므로
 * 만료·취소된 자격을 여기에 섞으면 안 된다.
 */
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
