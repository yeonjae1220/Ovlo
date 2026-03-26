package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CompleteOnboardingRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.RegisterMemberRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdateMemberProfileRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdateProfileImageRequest;
import me.yeonjae.ovlo.application.dto.command.CompleteOnboardingCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterMemberCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateMemberProfileCommand;
import me.yeonjae.ovlo.application.dto.command.UpdateProfileImageCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.member.CompleteOnboardingUseCase;
import me.yeonjae.ovlo.application.port.in.member.GetMemberQuery;
import me.yeonjae.ovlo.application.port.in.member.RegisterMemberUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateMemberProfileUseCase;
import me.yeonjae.ovlo.application.port.in.member.UpdateProfileImageUseCase;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Member", description = "회원 API")
@RestController
@RequestMapping("/api/v1/members")
public class MemberApiController {

    private final RegisterMemberUseCase registerMemberUseCase;
    private final UpdateMemberProfileUseCase updateMemberProfileUseCase;
    private final UpdateProfileImageUseCase updateProfileImageUseCase;
    private final WithdrawMemberUseCase withdrawMemberUseCase;
    private final CompleteOnboardingUseCase completeOnboardingUseCase;
    private final GetMemberQuery getMemberQuery;

    public MemberApiController(
            RegisterMemberUseCase registerMemberUseCase,
            UpdateMemberProfileUseCase updateMemberProfileUseCase,
            UpdateProfileImageUseCase updateProfileImageUseCase,
            WithdrawMemberUseCase withdrawMemberUseCase,
            CompleteOnboardingUseCase completeOnboardingUseCase,
            GetMemberQuery getMemberQuery
    ) {
        this.registerMemberUseCase = registerMemberUseCase;
        this.updateMemberProfileUseCase = updateMemberProfileUseCase;
        this.updateProfileImageUseCase = updateProfileImageUseCase;
        this.withdrawMemberUseCase = withdrawMemberUseCase;
        this.completeOnboardingUseCase = completeOnboardingUseCase;
        this.getMemberQuery = getMemberQuery;
    }

    @Operation(summary = "회원 가입")
    @PostMapping
    public ResponseEntity<MemberResult> register(@Valid @RequestBody RegisterMemberRequest request) {
        MemberResult result = registerMemberUseCase.register(
                new RegisterMemberCommand(
                        request.nickname(),
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

    @Operation(summary = "닉네임으로 회원 검색")
    @GetMapping("/search")
    public ResponseEntity<List<MemberResult>> search(
            @RequestParam String nickname,
            @AuthenticationPrincipal Long memberId
    ) {
        if (nickname == null || nickname.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<MemberResult> results = getMemberQuery.searchByNickname(nickname);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "회원 프로필 수정")
    @PutMapping("/{id}")
    public ResponseEntity<MemberResult> updateProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        if (!id.equals(memberId)) {
            throw new MemberException("본인 프로필만 수정할 수 있습니다", MemberException.ErrorType.FORBIDDEN);
        }
        MemberResult result = updateMemberProfileUseCase.updateProfile(
                new UpdateMemberProfileCommand(id, request.nickname(), request.bio())
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "프로필 이미지 수정")
    @PatchMapping("/{id}/profile-image")
    public ResponseEntity<MemberResult> updateProfileImage(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateProfileImageRequest request
    ) {
        if (!id.equals(memberId)) {
            throw new MemberException("본인 프로필만 수정할 수 있습니다", MemberException.ErrorType.FORBIDDEN);
        }
        MemberResult result = updateProfileImageUseCase.updateProfileImage(
                new UpdateProfileImageCommand(id, request.mediaId())
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Google 로그인 온보딩 완성 (신규 소셜 회원)")
    @PatchMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        completeOnboardingUseCase.completeOnboarding(new CompleteOnboardingCommand(
                memberId,
                request.hometown(),
                request.homeUniversityId(),
                request.majorName(),
                request.degreeType(),
                request.gradeLevel()
        ));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        if (!id.equals(memberId)) {
            throw new MemberException("본인 계정만 탈퇴할 수 있습니다", MemberException.ErrorType.FORBIDDEN);
        }
        withdrawMemberUseCase.withdraw(new WithdrawMemberCommand(id));
        return ResponseEntity.noContent().build();
    }
}
