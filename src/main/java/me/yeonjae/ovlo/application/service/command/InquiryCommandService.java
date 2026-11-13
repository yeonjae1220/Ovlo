package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AskQuestionCommand;
import me.yeonjae.ovlo.application.dto.command.InquiryFeedbackCommand;
import me.yeonjae.ovlo.application.dto.result.AskResult;
import me.yeonjae.ovlo.application.port.in.office.AskQuestionUseCase;
import me.yeonjae.ovlo.application.port.in.office.GiveInquiryFeedbackUseCase;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort.Candidate;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.application.port.out.office.NoticeSearchPort;
import me.yeonjae.ovlo.application.port.out.office.SaveInquiryPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import me.yeonjae.ovlo.application.service.office.InquiryHistory;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.InquiryFeedback;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeTranslation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 학생 질문 처리(설계 §5.2). LLM 호출은 트랜잭션 밖에서 하고 기록만 트랜잭션으로 한다.
 * 모든 Inquiry 를 저장한다 — NO_GROUNDS 도 지표의 원천이다(ADR-0006 5항).
 */
@Service
public class InquiryCommandService implements AskQuestionUseCase, GiveInquiryFeedbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(InquiryCommandService.class);
    /** 후보 본문이 너무 길면 토큰이 늘고 답변 품질은 오르지 않는다. 자료 등록 시 상한(20,000자)보다 훨씬 작게 둔다. */
    private static final int MAX_CANDIDATE_BODY_CHARS = 3000;

    private final me.yeonjae.ovlo.application.port.out.office.InquiryGuardPort guard;
    private final me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort admission;
    private final StudentOfficeAccessPolicy accessPolicy;
    private final NoticeSearchPort searchPort;
    private final AnswerGenerationPort answerPort;
    private final GroundedAnswerValidator validator;
    private final LoadInquiryPort loadInquiryPort;
    private final SaveInquiryPort saveInquiryPort;
    private final Clock clock;
    private final me.yeonjae.ovlo.application.service.office.NoticeAnswerFinalizer finalizer;
    private final me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance;
    private final int candidateLimit;
    private final int officeDailyLimit;

    public InquiryCommandService(StudentOfficeAccessPolicy accessPolicy, NoticeSearchPort searchPort,
                                 AnswerGenerationPort answerPort, GroundedAnswerValidator validator,
                                 LoadInquiryPort loadInquiryPort, SaveInquiryPort saveInquiryPort, Clock clock,
                                 me.yeonjae.ovlo.application.service.office.NoticeAnswerFinalizer finalizer,
                                 me.yeonjae.ovlo.application.service.office.NoticeGovernanceService governance,
                                 @Value("${ovlo.office.inquiry-candidate-limit:8}") int candidateLimit,
                                 @Value("${ovlo.office.inquiry-office-daily-limit:500}") int officeDailyLimit,
                                 me.yeonjae.ovlo.application.port.out.office.InquiryGuardPort guard,
                                 me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort admission) {
        this.guard=guard; this.admission=admission;
        this.accessPolicy = accessPolicy;
        this.searchPort = searchPort;
        this.answerPort = answerPort;
        this.validator = validator;
        this.loadInquiryPort = loadInquiryPort;
        this.saveInquiryPort = saveInquiryPort;
        this.clock = clock; this.finalizer=finalizer; this.governance=governance;
        this.candidateLimit = candidateLimit;
        this.officeDailyLimit = officeDailyLimit;
    }

    @Override
    public AskResult ask(AskQuestionCommand c) {
        String question = Inquiry.validateQuestion(c.question());
        accessPolicy.assertMember(c.memberId(), c.officeId());
        List<Inquiry> history = InquiryHistory.load(loadInquiryPort, c.officeId(), c.memberId(), c.previousInquiryId());
        if (!history.isEmpty()) {
            history.getLast().assertCanContinue();
            if (history.size() >= InquiryHistory.MAX_TURNS) throw InquiryHistory.conflict("새 대화를 시작해 주세요");
            if (loadInquiryPort.findByPreviousInquiryId(c.previousInquiryId()).isPresent())
                throw InquiryHistory.conflict("새 답변이 있습니다. 대화를 새로고침해 주세요");
        }
        assertOfficeDailyLimit(c.officeId(), me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock));
        // Invalid ownership/history or an already exhausted DB quota must not reserve paid-call budget.
        try (var lease=admission.acquire(c.officeId(), c.memberId())) {
            AskResult result=askAdmitted(c, question, history, lease);
            lease.complete();
            return result;
        }
    }

    private AskResult askAdmitted(AskQuestionCommand c, String question, List<Inquiry> history,
            me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort.Lease lease) {
        String searchQuestion = history.stream().skip(Math.max(0, history.size() - 2))
                .filter(i -> i.getOutcome() != me.yeonjae.ovlo.domain.office.model.InquiryOutcome.OUT_OF_SCOPE)
                .map(Inquiry::getQuestion).collect(java.util.stream.Collectors.joining(" "));
        searchQuestion = searchQuestion.isEmpty() ? question : searchQuestion + " " + question;
        Instant start = clock.instant();
        LocalDate today = me.yeonjae.ovlo.application.service.office.OfficeDates.today(clock);
        List<AnswerGenerationPort.Turn> promptHistory = history.stream()
                .skip(Math.max(0, history.size() - 6))
                .filter(i -> i.getOutcome() != me.yeonjae.ovlo.domain.office.model.InquiryOutcome.OUT_OF_SCOPE)
                .map(i -> new AnswerGenerationPort.Turn(i.getQuestion(), i.getAnswerText() == null ? null
                        : i.getAnswerText().substring(0, Math.min(2000, i.getAnswerText().length())))).toList();
        promptHistory = boundedHistory(promptHistory);
        var screened=guard.screen(question, promptHistory);
        if (screened == null) throw guardUnavailable();
        lease.usage(screened.inputTokens(), screened.outputTokens());
        if (!screened.allowed()) {
            Inquiry saved=record(Inquiry.outOfScope(c.officeId(), c.memberId(), c.lang(), question, start,
                    elapsed(start), screened.modelId()), history);
            return new AskResult(saved.getId(), saved.getOutcome(), c.lang(), null, List.of(), false);
        }


        List<Notice> candidates = searchPort.findCandidates(c.officeId(), c.lang(), searchQuestion, today, candidateLimit).stream()
                .filter(n -> n.isGroundingEligible(today, c.lang()))   // 최종 판정은 도메인(ADR-0006 2항)
                .filter(n -> governance.eligible(n,today))
                .limit(candidateLimit)
                .toList();
        List<Long> candidateIds = candidates.stream().map(Notice::getId).toList();

        if (candidates.isEmpty()) {
            Inquiry saved = record(Inquiry.noGrounds(c.officeId(), c.memberId(), c.lang(), question, candidateIds, start,
                    elapsed(start), null), history);
            return noGroundsResult(saved);
        }

        List<Candidate> context = IntStream.range(0, candidates.size())
                .mapToObj(i -> toCandidate(i + 1, candidates.get(i), c.lang()))
                .toList();
        context=boundedContext(context);
        var request=new AnswerGenerationPort.Request(question, c.lang(), context, promptHistory);
        AnswerGenerationPort.Result generated = answerPort.answer(request);
        lease.usage(generated.inputTokens(), generated.outputTokens());

        if (generated.refused()) {
            log.warn("[Inquiry] model refused office={} member={} model={}", c.officeId(), c.memberId(), generated.modelId());
            Inquiry saved = record(Inquiry.noGrounds(c.officeId(), c.memberId(), c.lang(), question, candidateIds, start,
                    elapsed(start), generated.modelId()), history);
            return noGroundsResult(saved);
        }

        GroundedAnswerValidator.Outcome outcome = validator.validate(generated.text(), context);
        if (!outcome.passed()) {
            // 강등 + 기록: 모델이 지시를 어긴 빈도를 봐야 프롬프트를 고칠 수 있다(ADR-0006 4항)
            log.warn("[Inquiry] answer rejected office={} reason={} model={} in={} out={}", c.officeId(),
                    outcome.rejection().reason(), generated.modelId(), generated.inputTokens(), generated.outputTokens());
            Inquiry saved = record(Inquiry.noGrounds(c.officeId(), c.memberId(), c.lang(), question, candidateIds, start,
                    elapsed(start), generated.modelId()), history);
            return noGroundsResult(saved);
        }

        var verified=guard.verify(request, outcome.validated().text());
        if (verified == null) throw guardUnavailable();
        lease.usage(verified.inputTokens(), verified.outputTokens());
        if (!verified.allowed()) {
            Inquiry saved=record(Inquiry.noGrounds(c.officeId(), c.memberId(), c.lang(), question, candidateIds, start,
                    elapsed(start), generated.modelId()), history);
            return noGroundsResult(saved);
        }
        GroundedAnswerValidator.Validated v = outcome.validated();
        List<me.yeonjae.ovlo.domain.office.model.NoticeCitation> evidence = v.citedIndices().stream().map(i -> {
            Notice n=candidates.get(i-1);
            return new me.yeonjae.ovlo.domain.office.model.NoticeCitation(i,n.getId(),n.getVersion(),titleIn(n,c.lang()),
                    n.getSource().publicSource()?n.getSourceUrl():null,n.getSource().type(),n.getSource().department(),
                    n.getReview().verifiedOn(),n.getExpiresAt());
        }).toList();
        Inquiry saved = finalizer.finish(Inquiry.answered(c.officeId(), c.memberId(), c.lang(), question, v.text(), v.citedNoticeIds(),
                candidateIds, start, elapsed(start), generated.modelId()), candidates, evidence, history);
        if(saved.getOutcome()!=me.yeonjae.ovlo.domain.office.model.InquiryOutcome.ANSWERED) return noGroundsResult(saved);
        log.info("[Inquiry] answered office={} inquiry={} cited={} latencyMs={} model={} in={} out={}", c.officeId(),
                saved.getId(), v.citedNoticeIds(), saved.getLatencyMs(), generated.modelId(), generated.inputTokens(),
                generated.outputTokens());

        List<AskResult.Citation> citations = evidence.stream().map(citation -> AskResult.Citation.from(citation,true)).toList();
        return new AskResult(saved.getId(), saved.getOutcome(), c.lang(), v.text(), citations, true);
    }

    @Override
    @Transactional
    public void feedback(InquiryFeedbackCommand c) {
        Inquiry inquiry = loadInquiryPort.findById(c.inquiryId())
                .filter(i -> i.getOfficeId().equals(c.officeId()))
                .orElseThrow(() -> new InquiryException("문의를 찾을 수 없습니다: id=" + c.inquiryId(),
                        InquiryException.ErrorType.NOT_FOUND));
        if (!inquiry.isOwnedBy(c.memberId())) {
            throw new InquiryException("본인의 문의에만 피드백할 수 있습니다", InquiryException.ErrorType.FORBIDDEN);
        }
        saveInquiryPort.saveFeedback(new InquiryFeedback(c.inquiryId(), c.helpful(), c.reason(), clock.instant()));
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private void assertOfficeDailyLimit(Long officeId, LocalDate today) {
        Instant startOfDay = today.atStartOfDay(me.yeonjae.ovlo.application.service.office.OfficeDates.ZONE).toInstant();
        long used = loadInquiryPort.countByOfficeSince(officeId, startOfDay);
        if (used >= officeDailyLimit) {
            throw new InquiryException("오늘 이 국제처의 질문 한도에 도달했습니다. 내일 다시 시도해 주세요",
                    InquiryException.ErrorType.RATE_LIMITED);
        }
    }

    @Transactional
    protected Inquiry record(Inquiry inquiry, List<Inquiry> history) {
        if (!history.isEmpty()) inquiry.following(history.getLast());
        return saveInquiryPort.save(inquiry);
    }

    private long elapsed(Instant start) {
        return Duration.between(start, clock.instant()).toMillis();
    }

    private static AskResult noGroundsResult(Inquiry saved) {
        return new AskResult(saved.getId(), saved.getOutcome(), saved.getLang(), null, List.of(), true);
    }

    /** 학생 언어의 최신 번역이 있으면 그것을, 없으면 원문을 후보 본문으로. 원문 언어(ko) 질문이면 원문. */
    private static Candidate toCandidate(int index, Notice n, NoticeLang lang) {
        Optional<NoticeTranslation> fresh = n.getTranslation(lang).filter(t -> !t.isStaleFor(n.getVersion()));
        String title = fresh.map(NoticeTranslation::getTitle).orElse(n.getTitle());
        String body = fresh.map(NoticeTranslation::getBody).orElse(n.getBody());
        if (body.length() > MAX_CANDIDATE_BODY_CHARS) {
            body = body.substring(0, MAX_CANDIDATE_BODY_CHARS);
        }
        return new Candidate(index, n.getId(), title, body, n.getSourceUrl());
    }

    private static String titleIn(Notice n, NoticeLang lang) {
        return n.getTranslation(lang).filter(t -> !t.isStaleFor(n.getVersion()))
                .map(NoticeTranslation::getTitle).orElse(n.getTitle());
    }
    private static InquiryException guardUnavailable() {
        return new InquiryException("안전한 답변을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.", InquiryException.ErrorType.UNAVAILABLE);
    }
    private static List<AnswerGenerationPort.Turn> boundedHistory(List<AnswerGenerationPort.Turn> history) {
        var kept=new java.util.ArrayList<AnswerGenerationPort.Turn>();
        int left=15000;
        for (int i=history.size()-1; i>=0; i--) {
            var t=history.get(i);
            int size=(t.question()+String.valueOf(t.answer())).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if(size>left) break;
            left-=size; kept.addFirst(t);
        }
        return List.copyOf(kept);
    }
    private static List<Candidate> boundedContext(List<Candidate> candidates) {
        var kept=new java.util.ArrayList<Candidate>();
        int left=30000;
        for(var c:candidates) {
            String title=c.title().substring(0, Math.min(c.title().length(), 200));
            String body=c.body();
            while((title+body).getBytes(java.nio.charset.StandardCharsets.UTF_8).length>left && !body.isEmpty())
                body=body.substring(0, body.offsetByCodePoints(body.length(), -1));
            if(body.isBlank()) break;
            kept.add(new Candidate(c.index(), c.noticeId(), title, body, c.sourceUrl()));
            left-=(title+body).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        }
        return List.copyOf(kept);
    }
}
