package me.yeonjae.ovlo.application.port.in.verification;

import me.yeonjae.ovlo.application.dto.command.ConfirmSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;

public interface ConfirmSchoolEmailVerificationUseCase {
    /** 코드 확인 성공 시 자격을 발급하고 갱신된 인증 현황을 반환한다. */
    VerificationStatusResult confirm(ConfirmSchoolEmailVerificationCommand command);
}
