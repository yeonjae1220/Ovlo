package me.yeonjae.ovlo.application.port.in.member;

import me.yeonjae.ovlo.application.dto.command.CompleteOnboardingCommand;

public interface CompleteOnboardingUseCase {
    void completeOnboarding(CompleteOnboardingCommand command);
}
