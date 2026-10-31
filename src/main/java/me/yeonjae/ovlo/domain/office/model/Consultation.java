package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.ConsultationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 담당자에게 넘어간 상담 (Aggregate Root). 설계 §4.5.
 *
 * 불변식
 * - 학생 동의(version + 정본 문구) 없이는 만들 수 없다
 * - 채팅방(chatRoomId)이 있어야 한다 — 상담 대화는 chat 도메인에 그대로 저장되고 여기는 상태·시각만 가진다
 * - OPEN → IN_PROGRESS(담당자 배정 필수) → RESOLVED → CLOSED. CLOSED 에서는 아무것도 바꿀 수 없다
 * - 학생은 종료 전 언제든 종료할 수 있다(상담 종료 요청)
 */
public class Consultation {

    public static final Duration AUTO_CLOSE_AFTER_RESOLVED = Duration.ofDays(7);

    private Long id;
    private final Long officeId;
    private final Long memberId;
    private final Long inquiryId;
    private final String consentVersion;
    private final String consentText;
    private final SharedContext sharedContext;
    private ConsultationStatus status;
    private Long assignedStaffMemberId;
    private final Long chatRoomId;
    private final Instant openedAt;
    private Instant firstStaffReplyAt;
    private Instant resolvedAt;
    private Instant closedAt;

    /** 학생이 동의한 공유 범위: 질문·자동 답변·언어·인증 여부. 이메일 등 식별 정보는 포함하지 않는다. */
    public record SharedContext(String question, String answerText, NoticeLang lang, boolean verifiedStudent) {}

    private Consultation(Long id, Long officeId, Long memberId, Long inquiryId, String consentVersion, String consentText,
                         SharedContext sharedContext, ConsultationStatus status, Long assignedStaffMemberId, Long chatRoomId,
                         Instant openedAt, Instant firstStaffReplyAt, Instant resolvedAt, Instant closedAt) {
        this.id = id;
        this.officeId = officeId;
        this.memberId = memberId;
        this.inquiryId = inquiryId;
        this.consentVersion = consentVersion;
        this.consentText = consentText;
        this.sharedContext = sharedContext;
        this.status = status;
        this.assignedStaffMemberId = assignedStaffMemberId;
        this.chatRoomId = chatRoomId;
        this.openedAt = openedAt;
        this.firstStaffReplyAt = firstStaffReplyAt;
        this.resolvedAt = resolvedAt;
        this.closedAt = closedAt;
    }

    public static Consultation open(Long officeId, Long memberId, Long inquiryId, String consentVersion,
                                    SharedContext sharedContext, Long chatRoomId, Instant now) {
        Objects.requireNonNull(officeId, "officeId는 필수입니다");
        Objects.requireNonNull(memberId, "memberId는 필수입니다");
        Objects.requireNonNull(sharedContext, "sharedContext는 필수입니다");
        Objects.requireNonNull(chatRoomId, "chatRoomId는 필수입니다");
        Objects.requireNonNull(now, "now는 필수입니다");
        if (consentVersion == null || consentVersion.isBlank()) {
            throw new ConsultationException("담당자에게 전달하려면 동의가 필요합니다", ConsultationException.ErrorType.CONSENT_REQUIRED);
        }
        String consentText = ConsultationConsent.textFor(consentVersion);
        return new Consultation(null, officeId, memberId, inquiryId, consentVersion, consentText, sharedContext,
                ConsultationStatus.OPEN, null, chatRoomId, now, null, null, null);
    }

    public static Consultation restore(Long id, Long officeId, Long memberId, Long inquiryId, String consentVersion,
                                       String consentText, SharedContext sharedContext, ConsultationStatus status,
                                       Long assignedStaffMemberId, Long chatRoomId, Instant openedAt, Instant firstStaffReplyAt,
                                       Instant resolvedAt, Instant closedAt) {
        return new Consultation(id, officeId, memberId, inquiryId, consentVersion, consentText, sharedContext, status,
                assignedStaffMemberId, chatRoomId, openedAt, firstStaffReplyAt, resolvedAt, closedAt);
    }

    // ── 전이 ──────────────────────────────────────────────────────────────────

    public void assign(Long staffMemberId, Instant now) {
        Objects.requireNonNull(staffMemberId, "staffMemberId는 필수입니다");
        if (status != ConsultationStatus.OPEN && status != ConsultationStatus.IN_PROGRESS) {
            throw invalid("배정은 OPEN·IN_PROGRESS 에서만 할 수 있습니다");
        }
        this.assignedStaffMemberId = staffMemberId;
        this.status = ConsultationStatus.IN_PROGRESS;
    }

    /** 담당자의 첫 답변 시각 — 응답 시간 지표. 이미 기록돼 있으면 그대로. */
    public void recordStaffReply(Instant now) {
        if (firstStaffReplyAt == null) {
            firstStaffReplyAt = now;
        }
    }

    public void resolve(Instant now) {
        if (status != ConsultationStatus.OPEN && status != ConsultationStatus.IN_PROGRESS) {
            throw invalid("해결 표시는 OPEN·IN_PROGRESS 에서만 할 수 있습니다");
        }
        this.status = ConsultationStatus.RESOLVED;
        this.resolvedAt = now;
    }

    /** 학생 확인 종료 또는 자동 종료. CLOSED 는 되돌릴 수 없다. */
    public void close(Instant now) {
        if (status == ConsultationStatus.CLOSED) {
            throw invalid("이미 종료된 상담입니다");
        }
        if (resolvedAt == null) {
            resolvedAt = now;
        }
        this.status = ConsultationStatus.CLOSED;
        this.closedAt = now;
    }

    public boolean isAutoCloseDue(Instant now) {
        return status == ConsultationStatus.RESOLVED && resolvedAt != null
                && !now.isBefore(resolvedAt.plus(AUTO_CLOSE_AFTER_RESOLVED));
    }

    public boolean isActive() {
        return status != ConsultationStatus.CLOSED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    private ConsultationException invalid(String message) {
        return new ConsultationException(message + ": " + status, ConsultationException.ErrorType.INVALID_STATE);
    }

    public void assignId(Long id) {
        this.id = id;
    }

    public Long getId()                      { return id; }
    public Long getOfficeId()                { return officeId; }
    public Long getMemberId()                { return memberId; }
    public Long getInquiryId()               { return inquiryId; }
    public String getConsentVersion()        { return consentVersion; }
    public String getConsentText()           { return consentText; }
    public SharedContext getSharedContext()  { return sharedContext; }
    public ConsultationStatus getStatus()    { return status; }
    public Long getAssignedStaffMemberId()   { return assignedStaffMemberId; }
    public Long getChatRoomId()              { return chatRoomId; }
    public Instant getOpenedAt()             { return openedAt; }
    public Instant getFirstStaffReplyAt()    { return firstStaffReplyAt; }
    public Instant getResolvedAt()           { return resolvedAt; }
    public Instant getClosedAt()             { return closedAt; }
}
