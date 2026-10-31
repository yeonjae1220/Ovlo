package me.yeonjae.ovlo.application.port.in.office;

import java.time.LocalDate;

/** 담당자 CSV 내보내기. 학생 식별 정보(memberId·이메일)는 어느 파일에도 싣지 않는다(설계 §5.4). */
public interface ExportOfficeDataQuery {
    String inquiriesCsv(Long officeId, LocalDate from, LocalDate to);
    String consultationsCsv(Long officeId);
    String noticesCsv(Long officeId);
}
