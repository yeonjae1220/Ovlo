package me.yeonjae.ovlo.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeKnowledgeUseCase;
import me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort;
import me.yeonjae.ovlo.domain.office.model.NoticeSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/office-admin/{officeId}")
public class NoticeKnowledgeApiController {
    private final ManageNoticeKnowledgeUseCase useCase;
    public NoticeKnowledgeApiController(ManageNoticeKnowledgeUseCase useCase) { this.useCase=useCase; }
    public record SourceRequest(@NotNull @Min(0) Long revision, @NotNull @Valid NoticeSource source) {}
    public record ReviewRequest(@NotNull @Min(0) Long revision,@NotBlank String outcome,@NotBlank @Size(max=2000) String evidence,
                                boolean official,boolean scopeChecked,boolean privacyChecked,boolean generalizable) {}
    public record ScopeRequest(Long id,@NotBlank @Size(max=200) String department,@NotBlank @Size(max=1000) String url,boolean enabled,@Min(0) long revision) {}
    @PutMapping("/notices/{noticeId}/source")
    public void source(@PathVariable Long officeId,@PathVariable Long noticeId,@AuthenticationPrincipal Long actor,@Valid @RequestBody SourceRequest request) {
        useCase.configure(officeId,noticeId,actor,request.revision(),request.source());
    }
    @PostMapping("/notices/{noticeId}/source-review")
    public void review(@PathVariable Long officeId,@PathVariable Long noticeId,@AuthenticationPrincipal Long actor,@Valid @RequestBody ReviewRequest r) {
        useCase.review(officeId,noticeId,actor,r.revision(),r.outcome(),r.evidence(),r.official(),r.scopeChecked(),r.privacyChecked(),r.generalizable());
    }
    @GetMapping("/notices/{noticeId}/history")
    public List<NoticeKnowledgePort.Audit> history(@PathVariable Long officeId,@PathVariable Long noticeId,@AuthenticationPrincipal Long actor) {
        return useCase.history(officeId,noticeId,actor);
    }
    @GetMapping("/review-queue")
    public List<ManageNoticeKnowledgeUseCase.ReviewTask> queue(@PathVariable Long officeId,@AuthenticationPrincipal Long actor) { return useCase.queue(officeId,actor); }
    @GetMapping("/source-scopes")
    public List<NoticeKnowledgePort.Scope> scopes(@PathVariable Long officeId,@AuthenticationPrincipal Long actor) { return useCase.scopes(officeId,actor); }
    @PutMapping("/source-scopes")
    public NoticeKnowledgePort.Scope scope(@PathVariable Long officeId,@AuthenticationPrincipal Long actor,@Valid @RequestBody ScopeRequest r) {
        return useCase.saveScope(officeId,actor,new NoticeKnowledgePort.Scope(r.id(),officeId,r.department(),r.url(),r.enabled(),r.revision()));
    }
}
