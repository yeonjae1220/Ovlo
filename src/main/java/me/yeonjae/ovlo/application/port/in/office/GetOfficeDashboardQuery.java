package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult;

import java.time.LocalDate;

public interface GetOfficeDashboardQuery {
    /** from·to 는 포함 구간(KST 날짜). null 이면 최근 30일. */
    OfficeDashboardResult dashboard(Long officeId, LocalDate from, LocalDate to);
}
