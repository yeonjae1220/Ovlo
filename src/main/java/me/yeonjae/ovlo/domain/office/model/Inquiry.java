package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.InquiryException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 학생 질문 1건과 그 결과 (Aggregate Root, append-only). 설계 §4.4.
 *
 * 불변식
 * - 질문은 비어 있지 않고 {@link #MAX_QUESTION_LENGTH} 자 이하
 * - ANSWERED 면 답변 본문이 있고 인용이 1개 이상이며 **인용 ⊆ 후보** (ADR-0006 의 최종 게이트)
 * - 생성 뒤 바뀌는 것은 ESCALATED 표시뿐
 */
public class Inquiry {

    public static final int MAX_QUESTION_LENGTH = 1000;

    private Long id;
    private final Long officeId;
    private final Long memberId;
    private final NoticeLang lang;
    private final String question;
    private InquiryOutcome outcome;
    private final String answerText;
    private final List<Long> citedNoticeIds;
    private final List<Long> candidateNoticeIds;
    private final Instant askedAt;
    private final long latencyMs;
    private final String modelId;

    private Inquiry(Long id, Long officeId, Long memberId, NoticeLang lang, String question, InquiryOutcome outcome,
                    String answerText, List<Long> citedNoticeIds, List<Long> candidateNoticeIds, Instant askedAt,
                    long latencyMs, String modelId) {
        this.id = id;
        this.officeId = officeId;
        this.memberId = memberId;
        this.lang = lang;
        this.question = question;
        this.outcome = outcome;
        this.answerText = answerText;
        this.citedNoticeIds = List.copyOf(citedNoticeIds);
        this.candidateNoticeIds = List.copyOf(candidateNoticeIds);
        this.askedAt = askedAt;
        this.latencyMs = latencyMs;
        this.modelId = modelId;
    }

    public static String validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new InquiryException("질문이 비어 있습니다", InquiryException.ErrorType.BAD_REQUEST);
        }
        String trimmed = question.trim();
        if (trimmed.length() > MAX_QUESTION_LENGTH) {
            throw new InquiryException("질문은 " + MAX_QUESTION_LENGTH + "자 이하여야 합니다",
                    InquiryException.ErrorType.BAD_REQUEST);
        }
        return trimmed;
    }

    public static Inquiry answered(Long officeId, Long memberId, NoticeLang lang, String question, String answerText,
                                   List<Long> citedNoticeIds, List<Long> candidateNoticeIds, Instant askedAt,
                                   long latencyMs, String modelId) {
        requireCommon(officeId, memberId, lang, askedAt);
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("ANSWERED 문의는 답변 본문이 있어야 합니다");
        }
        if (citedNoticeIds == null || citedNoticeIds.isEmpty()) {
            throw new IllegalArgumentException("ANSWERED 문의는 인용이 1개 이상이어야 합니다");
        }
        Set<Long> candidates = new HashSet<>(candidateNoticeIds == null ? List.of() : candidateNoticeIds);
        if (!candidates.containsAll(citedNoticeIds)) {
            throw new IllegalArgumentException("인용은 후보 집합의 부분집합이어야 합니다: cited=" + citedNoticeIds
                    + " candidates=" + candidateNoticeIds);
        }
        return new Inquiry(null, officeId, memberId, lang, validateQuestion(question), InquiryOutcome.ANSWERED,
                answerText.trim(), citedNoticeIds.stream().distinct().toList(), candidateNoticeIds, askedAt, latencyMs, modelId);
    }

    public static Inquiry noGrounds(Long officeId, Long memberId, NoticeLang lang, String question,
                                    List<Long> candidateNoticeIds, Instant askedAt, long latencyMs, String modelId) {
        requireCommon(officeId, memberId, lang, askedAt);
        return new Inquiry(null, officeId, memberId, lang, validateQuestion(question), InquiryOutcome.NO_GROUNDS, null,
                List.of(), candidateNoticeIds == null ? List.of() : candidateNoticeIds, askedAt, latencyMs, modelId);
    }

    public static Inquiry restore(Long id, Long officeId, Long memberId, NoticeLang lang, String question,
                                  InquiryOutcome outcome, String answerText, List<Long> citedNoticeIds,
                                  List<Long> candidateNoticeIds, Instant askedAt, long latencyMs, String modelId) {
        return new Inquiry(id, officeId, memberId, lang, question, outcome, answerText, citedNoticeIds,
                candidateNoticeIds, askedAt, latencyMs, modelId);
    }

    private static void requireCommon(Long officeId, Long memberId, NoticeLang lang, Instant askedAt) {
        Objects.requireNonNull(officeId, "officeId는 필수입니다");
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(lang, "lang은 필수입니다");
        Objects.requireNonNull(askedAt, "askedAt은 필수입니다");
    }

    /** S4: 학생 동의로 담당자에게 넘어감. */
    public void markEscalated() {
        this.outcome = InquiryOutcome.ESCALATED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId()                      { return id; }
    public Long getOfficeId()                { return officeId; }
    public Long getMemberId()                { return memberId; }
    public NoticeLang getLang()              { return lang; }
    public String getQuestion()              { return question; }
    public InquiryOutcome getOutcome()       { return outcome; }
    public String getAnswerText()            { return answerText; }
    public List<Long> getCitedNoticeIds()    { return citedNoticeIds; }
    public List<Long> getCandidateNoticeIds(){ return candidateNoticeIds; }
    public Instant getAskedAt()              { return askedAt; }
    public long getLatencyMs()               { return latencyMs; }
    public String getModelId()               { return modelId; }
}
