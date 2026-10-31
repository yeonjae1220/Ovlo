package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;

import java.time.Instant;
import java.util.List;

/** 담당자 문의 로그 행. 학생 식별 정보(memberId·이메일)는 싣지 않는다. */
public record InquiryLogResult(Long id, NoticeLang lang, String question, InquiryOutcome outcome, String answerText,
                               List<Long> citedNoticeIds, int candidateCount, Instant askedAt, long latencyMs,
                               String modelId, Boolean helpful, String feedbackReason) {
}
