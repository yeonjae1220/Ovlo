package me.yeonjae.ovlo.application.port.in.verification;

import me.yeonjae.ovlo.application.dto.command.RequestSchoolEmailVerificationCommand;
import me.yeonjae.ovlo.application.dto.result.VerificationRequestResult;

public interface RequestSchoolEmailVerificationUseCase {
    VerificationRequestResult request(RequestSchoolEmailVerificationCommand command);
}
