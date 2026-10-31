package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.NoticeRequest;
import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.command.ReviewNoticeTranslationCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateNoticeCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase;
import me.yeonjae.ovlo.application.service.office.OfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.shared.security.OfficePrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 담당자의 승인 자료 관리. 모든 메서드 첫 줄에서 OfficeAccessPolicy 로 범위를 확인한다(ADR-0005). */
@Tag(name = "Office Admin", description = "국제처 담당자 전용 — 승인 자료")
@RestController
@RequestMapping("/api/v1/office-admin/{officeId}/notices")
public class OfficeNoticeApiController {

    private final ManageNoticeUseCase manageNoticeUseCase;
    private final GetNoticeQuery getNoticeQuery;
    private final OfficeAccessPolicy accessPolicy;

    public OfficeNoticeApiController(ManageNoticeUseCase manageNoticeUseCase, GetNoticeQuery getNoticeQuery,
                                     OfficeAccessPolicy accessPolicy) {
        this.manageNoticeUseCase = manageNoticeUseCase;
        this.getNoticeQuery = getNoticeQuery;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "공지 목록 (상태·분류 필터)")
    @GetMapping
    public ResponseEntity<Page<NoticeSummaryResult>> list(@PathVariable Long officeId,
                                                          @RequestParam(required = false) NoticeStatus status,
                                                          @RequestParam(required = false) NoticeCategory category,
                                                          @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
                                                          Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(getNoticeQuery.findForStaff(officeId, status, category, pageable));
    }

    @Operation(summary = "공지 상세 (번역 상태 포함)")
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResult> get(@PathVariable Long officeId, @PathVariable Long noticeId,
                                            Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return getNoticeQuery.findDetail(officeId, noticeId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다", NoticeException.ErrorType.NOT_FOUND));
    }

    @Operation(summary = "공지 등록 (DRAFT)")
    @PostMapping
    public ResponseEntity<NoticeResult> register(@PathVariable Long officeId, @Valid @RequestBody NoticeRequest request,
                                                 @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        NoticeResult result = manageNoticeUseCase.register(new RegisterNoticeCommand(officeId, request.category(),
                request.title(), request.body(), request.sourceUrl(), request.attachmentMediaIds(), request.term(),
                request.effectiveFrom(), request.expiresAt(), memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "공지 수정 — 제목·본문이 바뀌면 DRAFT 로 돌아가 다시 승인해야 한다")
    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResult> update(@PathVariable Long officeId, @PathVariable Long noticeId,
                                               @Valid @RequestBody NoticeRequest request, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(manageNoticeUseCase.update(new UpdateNoticeCommand(officeId, noticeId, request.category(),
                request.title(), request.body(), request.sourceUrl(), request.attachmentMediaIds(), request.term(),
                request.effectiveFrom(), request.expiresAt())));
    }

    @Operation(summary = "승인 — 커밋 뒤 7개 언어 번역이 비동기로 생성된다")
    @PostMapping("/{noticeId}/approve")
    public ResponseEntity<NoticeResult> approve(@PathVariable Long officeId, @PathVariable Long noticeId,
                                                @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(manageNoticeUseCase.approve(new NoticeActionCommand(officeId, noticeId, String.valueOf(memberId))));
    }

    @Operation(summary = "보관 — 이후 변경 불가")
    @PostMapping("/{noticeId}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long officeId, @PathVariable Long noticeId,
                                        @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        manageNoticeUseCase.archive(new NoticeActionCommand(officeId, noticeId, String.valueOf(memberId)));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "번역 검토 완료 표시")
    @PostMapping("/{noticeId}/translations/{lang}/review")
    public ResponseEntity<Void> review(@PathVariable Long officeId, @PathVariable Long noticeId, @PathVariable String lang,
                                       Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        NoticeLang noticeLang = NoticeLang.fromCode(lang)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 언어입니다: " + lang));
        manageNoticeUseCase.reviewTranslation(new ReviewNoticeTranslationCommand(officeId, noticeId, noticeLang));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "없거나 stale 인 번역 다시 생성 (동기)")
    @PostMapping("/{noticeId}/retranslate")
    public ResponseEntity<NoticeResult> retranslate(@PathVariable Long officeId, @PathVariable Long noticeId,
                                                    @AuthenticationPrincipal Long memberId, Authentication authentication) {
        accessPolicy.assertStaff(OfficePrincipal.membershipOrNull(authentication), officeId);
        return ResponseEntity.ok(manageNoticeUseCase.retranslate(new NoticeActionCommand(officeId, noticeId, String.valueOf(memberId))));
    }
}
