package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.command.ReviewNoticeTranslationCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;

/** 담당자의 승인 자료 관리. 호출자 범위 검사(OfficeAccessPolicy)는 어댑터가 먼저 한다. */
public interface ManageNoticeUseCase {
    NoticeResult register(RegisterNoticeCommand command);
    NoticeResult update(UpdateNoticeCommand command);
    /** 승인하면 커밋 뒤 번역이 비동기로 생성된다. */
    NoticeResult approve(NoticeActionCommand command);
    void archive(NoticeActionCommand command);
    void reviewTranslation(ReviewNoticeTranslationCommand command);
    /** 없거나 stale 인 번역을 다시 생성한다(동기). 실패 언어는 결과의 missingLangs 로 드러난다. */
    NoticeResult retranslate(NoticeActionCommand command);
}
