package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CreateCommentRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CreatePostRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.ReactToPostRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UpdatePostRequest;
import me.yeonjae.ovlo.application.dto.command.CreateCommentCommand;
import me.yeonjae.ovlo.application.dto.command.CreatePostCommand;
import me.yeonjae.ovlo.application.dto.command.DeleteCommentCommand;
import me.yeonjae.ovlo.application.dto.command.DeletePostCommand;
import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UpdatePostCommand;
import me.yeonjae.ovlo.application.dto.result.CommentResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.in.post.CreateCommentUseCase;
import me.yeonjae.ovlo.application.port.in.post.CreatePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.DeleteCommentUseCase;
import me.yeonjae.ovlo.application.port.in.post.DeletePostUseCase;
import me.yeonjae.ovlo.application.port.in.post.GetPostQuery;
import me.yeonjae.ovlo.application.port.in.post.ReactToPostUseCase;
import me.yeonjae.ovlo.application.port.in.post.UpdatePostUseCase;
import me.yeonjae.ovlo.domain.post.model.PostId;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post", description = "게시글 API")
@Validated
@RestController
@RequestMapping("/api/v1/posts")
public class PostApiController {

    private final CreatePostUseCase createPostUseCase;
    private final UpdatePostUseCase updatePostUseCase;
    private final GetPostQuery getPostQuery;
    private final DeletePostUseCase deletePostUseCase;
    private final CreateCommentUseCase createCommentUseCase;
    private final DeleteCommentUseCase deleteCommentUseCase;
    private final ReactToPostUseCase reactToPostUseCase;

    public PostApiController(
            CreatePostUseCase createPostUseCase,
            UpdatePostUseCase updatePostUseCase,
            GetPostQuery getPostQuery,
            DeletePostUseCase deletePostUseCase,
            CreateCommentUseCase createCommentUseCase,
            DeleteCommentUseCase deleteCommentUseCase,
            ReactToPostUseCase reactToPostUseCase
    ) {
        this.createPostUseCase = createPostUseCase;
        this.updatePostUseCase = updatePostUseCase;
        this.getPostQuery = getPostQuery;
        this.deletePostUseCase = deletePostUseCase;
        this.createCommentUseCase = createCommentUseCase;
        this.deleteCommentUseCase = deleteCommentUseCase;
        this.reactToPostUseCase = reactToPostUseCase;
    }

    @Operation(summary = "게시글 작성")
    @PostMapping
    public ResponseEntity<PostResult> create(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        PostResult result = createPostUseCase.create(
                new CreatePostCommand(
                        request.boardId(),
                        memberId,
                        request.title(),
                        request.content()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "게시글 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<PostResult> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        PostResult result = getPostQuery.getById(new PostId(id), memberId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시글 수정")
    @PutMapping("/{id}")
    public ResponseEntity<PostResult> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        PostResult result = updatePostUseCase.update(
                new UpdatePostCommand(id, memberId, request.title(), request.content())
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        deletePostUseCase.delete(new DeletePostCommand(id, memberId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping("/{id}/comments")
    public ResponseEntity<PageResult<CommentResult>> getComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        return ResponseEntity.ok(getPostQuery.getComments(new PostId(id), page, size));
    }

    @Operation(summary = "댓글 작성")
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResult> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        CommentResult result = createCommentUseCase.createComment(
                new CreateCommentCommand(id, memberId, request.content())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long memberId
    ) {
        deleteCommentUseCase.deleteComment(new DeleteCommentCommand(id, commentId, memberId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 반응 (좋아요/싫어요)")
    @PostMapping("/{id}/reactions")
    public ResponseEntity<Void> react(
            @PathVariable Long id,
            @Valid @RequestBody ReactToPostRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        reactToPostUseCase.react(
                new ReactToPostCommand(id, memberId, request.reactionType())
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 반응 취소")
    @DeleteMapping("/{id}/reactions")
    public ResponseEntity<Void> unreact(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        reactToPostUseCase.unreact(new UnreactToPostCommand(id, memberId));
        return ResponseEntity.noContent().build();
    }
}
