package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.FollowRequest;
import me.yeonjae.ovlo.application.dto.command.FollowCommand;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.follow.FollowMemberUseCase;
import me.yeonjae.ovlo.application.port.in.follow.GetFollowQuery;
import me.yeonjae.ovlo.application.port.in.follow.UnfollowMemberUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Follow", description = "팔로우 API")
@RestController
@RequestMapping("/api/v1/follows")
public class FollowApiController {

    private final FollowMemberUseCase followMemberUseCase;
    private final UnfollowMemberUseCase unfollowMemberUseCase;
    private final GetFollowQuery getFollowQuery;

    public FollowApiController(
            FollowMemberUseCase followMemberUseCase,
            UnfollowMemberUseCase unfollowMemberUseCase,
            GetFollowQuery getFollowQuery
    ) {
        this.followMemberUseCase = followMemberUseCase;
        this.unfollowMemberUseCase = unfollowMemberUseCase;
        this.getFollowQuery = getFollowQuery;
    }

    @Operation(summary = "팔로우")
    @PostMapping
    public ResponseEntity<Void> follow(
            @Valid @RequestBody FollowRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        followMemberUseCase.follow(new FollowCommand(memberId, request.followeeId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "언팔로우")
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long followeeId,
            @AuthenticationPrincipal Long memberId
    ) {
        unfollowMemberUseCase.unfollow(new FollowCommand(memberId, followeeId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 회원의 팔로워 목록 조회")
    @GetMapping("/followers/{memberId}")
    public ResponseEntity<List<MemberResult>> getFollowers(@PathVariable Long memberId) {
        return ResponseEntity.ok(getFollowQuery.getFollowers(memberId));
    }

    @Operation(summary = "특정 회원의 팔로잉 목록 조회")
    @GetMapping("/followings/{memberId}")
    public ResponseEntity<List<MemberResult>> getFollowings(@PathVariable Long memberId) {
        return ResponseEntity.ok(getFollowQuery.getFollowings(memberId));
    }
}
