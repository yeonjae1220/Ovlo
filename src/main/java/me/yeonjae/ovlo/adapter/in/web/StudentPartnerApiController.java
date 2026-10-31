package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.yeonjae.ovlo.application.dto.result.PartnerCardResult;
import me.yeonjae.ovlo.application.port.in.office.StudentPartnerUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 학생용 정착 제휴 카드. 화면은 항상 "제휴" 표시를 붙인다(설계 §6.3).
 * 목적지 URL 은 클릭 API 가 집계한 뒤 돌려준다 — Bearer 인증이라 서버 302 리다이렉트는 링크로 못 쓴다.
 */
@Tag(name = "Office", description = "국제처 — 학생용 정착 제휴")
@RestController
@RequestMapping("/api/v1/office/{officeId}/partners")
public class StudentPartnerApiController {

    private final StudentPartnerUseCase useCase;
    private final me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics;

    public StudentPartnerApiController(StudentPartnerUseCase useCase, me.yeonjae.ovlo.shared.metrics.OfficeMetrics metrics) {
        this.useCase = useCase;
        this.metrics = metrics;
    }

    @Operation(summary = "이 대학이 켠 분류의 제휴 카드 (유심→보험→숙소→픽업 순)")
    @GetMapping
    public ResponseEntity<List<PartnerCardResult>> list(@PathVariable Long officeId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(useCase.forStudent(officeId, memberId));
    }

    @Operation(summary = "클릭 집계 후 목적지 URL 반환")
    @PostMapping("/{partnerId}/click")
    public ResponseEntity<Map<String, String>> click(@PathVariable Long officeId, @PathVariable Long partnerId,
                                                     @AuthenticationPrincipal Long memberId) {
        StudentPartnerUseCase.ClickResult result = useCase.click(officeId, partnerId, memberId);
        metrics.partnerClick(result.category());
        return ResponseEntity.ok(Map.of("url", result.url()));
    }
}
