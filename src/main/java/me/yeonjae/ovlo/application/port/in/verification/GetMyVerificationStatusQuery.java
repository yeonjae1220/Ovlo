package me.yeonjae.ovlo.application.port.in.verification;

import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;

public interface GetMyVerificationStatusQuery {
    VerificationStatusResult getByMemberId(Long memberId);
}
