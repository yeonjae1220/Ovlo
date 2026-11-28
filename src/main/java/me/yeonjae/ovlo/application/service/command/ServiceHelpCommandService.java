package me.yeonjae.ovlo.application.service.command;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import me.yeonjae.ovlo.application.dto.command.AskServiceHelpCommand;
import me.yeonjae.ovlo.application.dto.result.ServiceHelpAnswerResult;
import me.yeonjae.ovlo.application.port.in.help.AskServiceHelpUseCase;
import me.yeonjae.ovlo.application.port.out.help.LoadHelpArticlesPort;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpAdmissionPort;
import me.yeonjae.ovlo.application.port.out.help.ServiceHelpLlmPort;
import me.yeonjae.ovlo.application.port.out.office.AnswerGenerationPort;
import me.yeonjae.ovlo.application.port.out.office.InquiryAdmissionPort;
import me.yeonjae.ovlo.application.service.office.GroundedAnswerValidator;
import me.yeonjae.ovlo.domain.help.model.HelpAnswerOutcome;
import me.yeonjae.ovlo.domain.help.model.HelpArticle;
import me.yeonjae.ovlo.domain.help.model.HelpQuestion;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 서비스 도움말 질문. 운영팀이 쓴 도움말 문서만 근거로 답하고, 근거가 없으면 답하지 않는다.
 * 국제처 질문과 같은 안전 단계(분류 → 답변 → 인용 검사 → 독립 검증)를 밟되, 질문 원문은 저장·로그하지 않는다.
 */
@Service
public class ServiceHelpCommandService implements AskServiceHelpUseCase {

    private static final Logger log = LoggerFactory.getLogger(ServiceHelpCommandService.class);

    private final LoadHelpArticlesPort articlesPort;
    private final ServiceHelpLlmPort llm;
    private final ServiceHelpAdmissionPort admission;
    private final GroundedAnswerValidator validator;
    private final Map<HelpAnswerOutcome, Counter> outcomes = new EnumMap<>(HelpAnswerOutcome.class);

    public ServiceHelpCommandService(LoadHelpArticlesPort articlesPort, ServiceHelpLlmPort llm,
                                     ServiceHelpAdmissionPort admission, GroundedAnswerValidator validator,
                                     MeterRegistry registry) {
        this.articlesPort = articlesPort;
        this.llm = llm;
        this.admission = admission;
        this.validator = validator;
        for (HelpAnswerOutcome outcome : HelpAnswerOutcome.values()) {
            outcomes.put(outcome, Counter.builder("ovlo.help.answers").tag("outcome", outcome.name())
                    .description("Service help assistant outcomes").register(registry));
        }
    }

    @Override
    public ServiceHelpAnswerResult ask(AskServiceHelpCommand command) {
        HelpQuestion question = new HelpQuestion(command.question());
        List<HelpArticle> articles = articlesPort.loadAll();
        try (InquiryAdmissionPort.Lease lease = admission.acquireServiceHelp(command.memberId())) {
            ServiceHelpAnswerResult result = askAdmitted(question, command.lang(), articles, lease);
            lease.complete();
            outcomes.get(result.outcome()).increment();
            log.info("[Help] outcome={} cited={}", result.outcome(),
                    result.citations().stream().map(ServiceHelpAnswerResult.Citation::articleId).toList());
            return result;
        }
    }

    private ServiceHelpAnswerResult askAdmitted(HelpQuestion question, NoticeLang lang, List<HelpArticle> articles,
                                                InquiryAdmissionPort.Lease lease) {
        ServiceHelpLlmPort.Verdict screened = required(llm.screenHelp(question.value()));
        lease.usage(screened.inputTokens(), screened.outputTokens());
        if (!screened.allowed()) {
            return ServiceHelpAnswerResult.withoutAnswer(HelpAnswerOutcome.OUT_OF_SCOPE, lang);
        }

        var request = new ServiceHelpLlmPort.Request(question.value(), lang, references(articles));
        ServiceHelpLlmPort.Answer generated = llm.answerHelp(request);
        lease.usage(generated.inputTokens(), generated.outputTokens());
        if (generated.refused()) {
            return noGrounds(lang);
        }

        GroundedAnswerValidator.Outcome checked = validator.validate(generated.text(), candidates(request));
        if (!checked.passed()) {
            log.info("[Help] answer rejected reason={} model={}", checked.rejection().code(), generated.modelId());
            return noGrounds(lang);
        }

        GroundedAnswerValidator.Validated answer = checked.validated();
        ServiceHelpLlmPort.Verdict verified = required(llm.verifyHelp(request, answer.text()));
        lease.usage(verified.inputTokens(), verified.outputTokens());
        if (!verified.allowed()) {
            return noGrounds(lang);
        }

        List<ServiceHelpAnswerResult.Citation> citations = answer.citedIndices().stream()
                .map(i -> new ServiceHelpAnswerResult.Citation(i, articles.get(i - 1).id(), articles.get(i - 1).title()))
                .toList();
        return new ServiceHelpAnswerResult(HelpAnswerOutcome.ANSWERED, lang, answer.text(), citations);
    }

    private static List<ServiceHelpLlmPort.Reference> references(List<HelpArticle> articles) {
        return IntStream.range(0, articles.size())
                .mapToObj(i -> new ServiceHelpLlmPort.Reference(i + 1, articles.get(i).title(), articles.get(i).body()))
                .toList();
    }

    /** 인용 검사기는 국제처와 같은 것을 쓴다. 도움말은 외부 URL 이 없으므로 sourceUrl 은 비운다. */
    private static List<AnswerGenerationPort.Candidate> candidates(ServiceHelpLlmPort.Request request) {
        return request.references().stream()
                .map(r -> new AnswerGenerationPort.Candidate(r.index(), (long) r.index(), r.title(), r.body(), null))
                .toList();
    }

    private static ServiceHelpAnswerResult noGrounds(NoticeLang lang) {
        return ServiceHelpAnswerResult.withoutAnswer(HelpAnswerOutcome.NO_GROUNDS, lang);
    }

    private static ServiceHelpLlmPort.Verdict required(ServiceHelpLlmPort.Verdict verdict) {
        if (verdict == null) {
            throw new InquiryException("안전한 답변을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    InquiryException.ErrorType.UNAVAILABLE);
        }
        return verdict;
    }
}
