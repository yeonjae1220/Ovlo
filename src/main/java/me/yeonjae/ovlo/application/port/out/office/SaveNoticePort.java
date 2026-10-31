package me.yeonjae.ovlo.application.port.out.office;

import me.yeonjae.ovlo.domain.office.model.Notice;

public interface SaveNoticePort {
    /** 공지와 번역을 함께 저장한다(애그리거트 단위). */
    Notice save(Notice notice);
}
