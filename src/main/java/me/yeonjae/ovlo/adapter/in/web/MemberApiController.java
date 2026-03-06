package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.RegisterMemberRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdateMemberProfileRequest;
import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.member.GetMemberQuery;
import me.yeonjae.ovlo.application.port.in.member.RegisterMemberUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateMemberProfileUseCase;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/v1/members")
public class MemberApiController {

    private final RegisterMemberUseCase registerMemberUseCase;
    private final UpdateMemberProfileUseCase updateMemberProfileUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;
    private final GetMemberQuery getMemberQuery;

    public MemberApiController(
            RegisterMemberUseCase registerMemberUseCase,
            UpdateMemberProfileUseCase updateMemberProfileUseCase,
            WithdrawMemberUseCase withdrawMemberUseCase,
            GetMemberQuery getMemberQuery
    ) {
        this.registerMemberUseCase = registerMemberUseCase;
        this.updateMemberProfileUseCase = updateMemberProfileUseCase;
        this.withdrawMemberUseCase = withdrawMemberUseCase;
        this.getMemberQuery = getMemberQuery;
    }

    @Operation(summary = "회원 가입")
    @PostMapping
    public ResponseEntity<MemberResult> register(@Valid @RequestBody RegisterMemberRequest request) {
        MemberResult result = registerMemberUseCase.register(
                new RegisterMemberCommand(
                        request.name(),
                        request.hometown(),
                        request.email(),
                        request.password(),
                        request.homeUniversityId(),
                        request.majorName(),
                        request.degreeType(),
                        request.gradeLevel()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "회원 조회")
    @GetMapping("/{id}")
    public ResponseEntity<MemberResult> getById(@PathVariable Long id) {
        MemberResult result = getMemberQuery.getById(new MemberId(id));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "회원 프로필 수정")
    @PutMapping("/{id}")
    public ResponseEntity<MemberResult> updateProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        MemberResult result = updateMemberProfileUseCase.updateProfile(
                new UpdateMemberProfileCommand(
                        id,
                        request.name(),
                        request.hometown(),
                        request.majorName(),
                        request.degreeType(),
                        request.gradeLevel()
                )
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        withdrawMemberUseCase.withdraw(new WithdrawMemberCommand(id));
        return ResponseEntity.noContent().build();
    }
}
