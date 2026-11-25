package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.application.dto.command.ReportContentCommand;
import me.yeonjae.ovlo.application.dto.result.BlockedMemberPageResult;
import me.yeonjae.ovlo.application.port.in.moderation.BlockMemberUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.GetBlockedMembersQuery;
import me.yeonjae.ovlo.application.port.in.moderation.ReportContentUseCase;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Moderation", description = "콘텐츠 신고 · 회원 차단")
@Validated
@RestController
@RequestMapping("/api/v1")
public class ModerationApiController {

    private final ReportContentUseCase reportContentUseCase;
    private final BlockMemberUseCase blockMemberUseCase;
    private final GetBlockedMembersQuery getBlockedMembersQuery;
    private final RateLimiterService rateLimiter;

    public ModerationApiController(ReportContentUseCase reportContentUseCase, BlockMemberUseCase blockMemberUseCase,
                                   GetBlockedMembersQuery getBlockedMembersQuery, RateLimiterService rateLimiter) {
        this.reportContentUseCase = reportContentUseCase;
        this.blockMemberUseCase = blockMemberUseCase;
        this.getBlockedMembersQuery = getBlockedMembersQuery;
        this.rateLimiter = rateLimiter;
    }

    public record ReportRequest(
            @NotNull ReportTargetType targetType,
            @NotNull @Positive Long targetId,
            @NotNull ReportReason reason,
            @Size(max = ContentReport.MAX_DETAIL_LENGTH) String detail
    ) {}

    @Operation(summary = "콘텐츠 신고", description = "같은 대상을 다시 신고해도 204 를 돌려주고 첫 신고를 유지한다.")
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@AuthenticationPrincipal Long memberId, @Valid @RequestBody ReportRequest request) {
        rateLimiter.checkReportRate(memberId);
        reportContentUseCase.report(new ReportContentCommand(
                memberId, request.targetType(), request.targetId(), request.reason(), request.detail()));
    }

    @Operation(summary = "내가 차단한 회원 목록")
    @GetMapping("/blocks")
    public BlockedMemberPageResult blockedMembers(@AuthenticationPrincipal Long memberId,
                                                  @RequestParam(required = false) @Positive Long before) {
        return getBlockedMembersQuery.blockedMembers(memberId, before);
    }

    public record BlockStatus(boolean blockedByMe) {}

    @Operation(summary = "내가 이 회원을 차단했는지")
    @GetMapping("/blocks/{targetId}")
    public BlockStatus blockStatus(@AuthenticationPrincipal Long memberId, @PathVariable @Positive long targetId) {
        return new BlockStatus(getBlockedMembersQuery.isBlockedByMe(memberId, targetId));
    }

    @Operation(summary = "회원 차단", description = "서로의 게시글·댓글이 보이지 않고 DM 을 주고받을 수 없다. 팔로우도 끊긴다.")
    @PutMapping("/blocks/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@AuthenticationPrincipal Long memberId, @PathVariable @Positive long targetId) {
        blockMemberUseCase.block(memberId, targetId);
    }

    @Operation(summary = "회원 차단 해제")
    @DeleteMapping("/blocks/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@AuthenticationPrincipal Long memberId, @PathVariable @Positive long targetId) {
        blockMemberUseCase.unblock(memberId, targetId);
    }
}
