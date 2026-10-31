package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.PartnerRequest;
import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 플랫폼 ADMIN 전용(/api/v1/admin/** → hasAuthority("ADMIN")). 제휴 링크 등록·수정·켜기/끄기. */
@Tag(name = "Admin", description = "어드민 API (ADMIN role 전용)")
@RestController
@RequestMapping("/api/v1/admin/partners")
public class PartnerAdminApiController {

    private final ManagePartnerUseCase useCase;

    public PartnerAdminApiController(ManagePartnerUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "제휴 목록")
    @GetMapping
    public ResponseEntity<List<PartnerResult>> list() {
        return ResponseEntity.ok(useCase.findAll());
    }

    @Operation(summary = "제휴 등록 (기본 꺼짐)")
    @PostMapping
    public ResponseEntity<PartnerResult> create(@Valid @RequestBody PartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(toCommand(request)));
    }

    @Operation(summary = "제휴 수정")
    @PutMapping("/{partnerId}")
    public ResponseEntity<PartnerResult> update(@PathVariable Long partnerId, @Valid @RequestBody PartnerRequest request) {
        return ResponseEntity.ok(useCase.update(partnerId, toCommand(request)));
    }

    @Operation(summary = "제휴 켜기")
    @PostMapping("/{partnerId}/enable")
    public ResponseEntity<Void> enable(@PathVariable Long partnerId) {
        useCase.setEnabled(partnerId, true);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "제휴 끄기")
    @PostMapping("/{partnerId}/disable")
    public ResponseEntity<Void> disable(@PathVariable Long partnerId) {
        useCase.setEnabled(partnerId, false);
        return ResponseEntity.noContent().build();
    }

    private static UpsertPartnerCommand toCommand(PartnerRequest r) {
        return new UpsertPartnerCommand(r.category(), r.name(), r.description(), r.targetUrl(), r.sortOrder());
    }
}
