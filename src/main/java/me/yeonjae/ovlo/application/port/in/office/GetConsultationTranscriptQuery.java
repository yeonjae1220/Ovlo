package me.yeonjae.ovlo.application.port.in.office;

import me.yeonjae.ovlo.application.dto.result.TranscriptMessageResult;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.util.List;

/**
 * 상담 대화를 요청 언어로. 상대방 메시지 중 번역이 없는 것은 그 자리에서 번역해 저장한다(메시지·언어당 1회).
 * staff=true 면 호출자가 그 Office 담당자임을 어댑터가 이미 확인한 것이다.
 */
public interface GetConsultationTranscriptQuery {
    List<TranscriptMessageResult> transcript(Long officeId, Long consultationId, Long requesterMemberId, boolean staff,
                                             NoticeLang lang, int limit);
}
