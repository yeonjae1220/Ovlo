package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.LocalDate;
import java.util.List;

/**
 * 학생에게 돌려주는 답변. answerText 는 [N] 인용 번호를 포함하며 N 은 citations 의 index 와 같다.
 * NO_GROUNDS 면 answerText 는 null 이고 화면은 "국제처에 문의/상담 요청"을 보여 준다.
 */
public record AskResult(Long inquiryId, InquiryOutcome outcome, NoticeLang lang, String answerText,
                        List<Citation> citations, boolean canEscalate) {

    public record Citation(int index, Long noticeId, String title, String sourceUrl, LocalDate expiresAt,
                           Integer noticeVersion, String sourceType, String department, LocalDate verifiedOn,
                           boolean currentlyValid, boolean versionRecorded) {
        public Citation(int index,Long noticeId,String title,String sourceUrl,LocalDate expiresAt) {
            this(index,noticeId,title,sourceUrl,expiresAt,null,null,null,null,false,false);
        }
        public static Citation from(me.yeonjae.ovlo.domain.office.model.NoticeCitation c,boolean valid) {
            return new Citation(c.index(),c.noticeId(),c.title(),c.sourceUrl(),c.expiresAt(),c.version(),
                    c.sourceType().name(),c.department(),c.verifiedOn(),valid,true);
        }
    }
}
