package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Retained own history is independent of current school credentials. Authentication remains required. */
@RestController
@RequestMapping("/api/v1/office/my-consultations")
public class MyConsultationApiController {
    private final GetConsultationQuery query;
    public MyConsultationApiController(GetConsultationQuery query) { this.query=query; }
    @GetMapping public Page<ConsultationResult> mine(@AuthenticationPrincipal Long memberId,
            @PageableDefault(size=20) Pageable pageable) {
        return query.findOwnHistory(memberId,pageable);
    }
    @GetMapping("/{id}") public ConsultationResult detail(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
        return query.findOwnDetail(id,memberId).orElseThrow(() ->
            new ConsultationException("상담을 찾을 수 없습니다",ConsultationException.ErrorType.NOT_FOUND));
    }
}
