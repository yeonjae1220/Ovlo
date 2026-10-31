package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Office;

public interface SaveOfficePort {
    /** Office 와 담당자 목록을 함께 저장한다(애그리거트 단위). */
    Office save(Office office);
}
