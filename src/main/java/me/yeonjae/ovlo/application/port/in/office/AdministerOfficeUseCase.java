package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.CreateOfficeCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;

/** 플랫폼 ADMIN 전용: Office 생성·정지·재개. */
public interface AdministerOfficeUseCase {
    OfficeResult create(CreateOfficeCommand command);
    void suspend(Long officeId);
    void activate(Long officeId);
}
