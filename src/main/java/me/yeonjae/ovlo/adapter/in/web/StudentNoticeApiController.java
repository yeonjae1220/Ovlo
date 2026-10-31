package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.result.StudentNoticeResult;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.office.exception.NoticeException;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 학생이 보는 승인 공지. 소속은 StudentOfficeAccessPolicy(활성 인증 자격)가 확인한다. */
@Tag(name = "Office", description = "국제처 — 학생용 승인 공지")
@RestController
@RequestMapping("/api/v1/office/{officeId}/notices")
public class StudentNoticeApiController {

    private final GetNoticeQuery getNoticeQuery;
    private final StudentOfficeAccessPolicy accessPolicy;

    public StudentNoticeApiController(GetNoticeQuery getNoticeQuery, StudentOfficeAccessPolicy accessPolicy) {
        this.getNoticeQuery = getNoticeQuery;
        this.accessPolicy = accessPolicy;
    }

    @Operation(summary = "오늘 유효한 승인 공지 (내 언어, 없으면 원문)")
    @GetMapping
    public ResponseEntity<List<StudentNoticeResult>> list(@PathVariable Long officeId,
                                                          @RequestParam(required = false) NoticeCategory category,
                                                          @RequestParam(defaultValue = "ko") String lang,
                                                          @AuthenticationPrincipal Long memberId) {
        accessPolicy.assertMember(memberId, officeId);
        return ResponseEntity.ok(getNoticeQuery.findForStudent(officeId, category, resolve(lang)));
    }

    @Operation(summary = "공지 상세")
    @GetMapping("/{noticeId}")
    public ResponseEntity<StudentNoticeResult> get(@PathVariable Long officeId, @PathVariable Long noticeId,
                                                   @RequestParam(defaultValue = "ko") String lang,
                                                   @AuthenticationPrincipal Long memberId) {
        accessPolicy.assertMember(memberId, officeId);
        return getNoticeQuery.findStudentDetail(officeId, noticeId, resolve(lang))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoticeException("공지를 찾을 수 없습니다", NoticeException.ErrorType.NOT_FOUND));
    }

    private static NoticeLang resolve(String lang) {
        return NoticeLang.fromCode(lang).orElse(NoticeLang.KO);
    }
}
