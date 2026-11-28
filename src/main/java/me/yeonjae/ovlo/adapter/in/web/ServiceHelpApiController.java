package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.ServiceHelpAskRequest;
import me.yeonjae.ovlo.application.dto.command.AskServiceHelpCommand;
import me.yeonjae.ovlo.application.dto.result.HelpArticleResult;
import me.yeonjae.ovlo.application.dto.result.ServiceHelpAnswerResult;
import me.yeonjae.ovlo.application.port.in.help.AskServiceHelpUseCase;
import me.yeonjae.ovlo.application.port.in.help.ListHelpArticlesQuery;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 서비스 도움말. 문서는 공개, AI 질문은 로그인 회원만(회원별 요청 제한 → 유스케이스의 전역 예약). */
@Tag(name = "Help", description = "서비스 도움말")
@RestController
@RequestMapping("/api/v1/help")
public class ServiceHelpApiController {

    private final ListHelpArticlesQuery articlesQuery;
    private final AskServiceHelpUseCase askUseCase;
    private final RateLimiterService rateLimiter;

    public ServiceHelpApiController(ListHelpArticlesQuery articlesQuery, AskServiceHelpUseCase askUseCase,
                                    RateLimiterService rateLimiter) {
        this.articlesQuery = articlesQuery;
        this.askUseCase = askUseCase;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "도움말 문서 목록")
    @GetMapping("/articles")
    public ResponseEntity<List<HelpArticleResult>> articles() {
        return ResponseEntity.ok(articlesQuery.list());
    }

    @Operation(summary = "도움말 AI 질문 — 도움말 문서만 근거로 답하고 근거가 없으면 NO_GROUNDS")
    @PostMapping("/ask")
    public ResponseEntity<ServiceHelpAnswerResult> ask(@Valid @RequestBody ServiceHelpAskRequest request,
                                                       @AuthenticationPrincipal Long memberId) {
        rateLimiter.checkServiceHelpRate(memberId);
        NoticeLang lang = NoticeLang.fromCode(request.lang()).orElse(NoticeLang.KO);
        return ResponseEntity.ok(askUseCase.ask(new AskServiceHelpCommand(memberId, request.question(), lang)));
    }
}
