package me.yeonjae.ovlo.application.port.in.moderation;

import me.yeonjae.ovlo.application.dto.command.ReportContentCommand;

public interface ReportContentUseCase {
    /** 같은 회원이 같은 대상을 다시 신고하면 첫 신고를 유지하고 조용히 끝난다. */
    void report(ReportContentCommand command);
}
