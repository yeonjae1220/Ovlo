package me.yeonjae.ovlo.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.yeonjae.ovlo.adapter.in.web.dto.request.CreateBoardRequest;
import me.yeonjae.ovlo.application.dto.command.CreateBoardCommand;
import me.yeonjae.ovlo.application.dto.command.SearchBoardCommand;
import me.yeonjae.ovlo.application.dto.command.SubscribeBoardCommand;
import me.yeonjae.ovlo.application.dto.result.BoardPageResult;
import me.yeonjae.ovlo.application.dto.result.BoardResult;
import me.yeonjae.ovlo.application.port.in.board.CreateBoardUseCase;
import me.yeonjae.ovlo.application.port.in.board.GetBoardQuery;
import me.yeonjae.ovlo.application.port.in.board.SearchBoardQuery;
import me.yeonjae.ovlo.application.port.in.board.SubscribeBoardUseCase;
import me.yeonjae.ovlo.application.port.in.board.UnsubscribeBoardUseCase;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Board", description = "게시판 API")
@RestController
@RequestMapping("/api/v1/boards")
public class BoardApiController {

    private final CreateBoardUseCase createBoardUseCase;
    private final GetBoardQuery getBoardQuery;
    private final SearchBoardQuery searchBoardQuery;
    private final SubscribeBoardUseCase subscribeBoardUseCase;
    private final UnsubscribeBoardUseCase unsubscribeBoardUseCase;

    public BoardApiController(
            CreateBoardUseCase createBoardUseCase,
            GetBoardQuery getBoardQuery,
            SearchBoardQuery searchBoardQuery,
            SubscribeBoardUseCase subscribeBoardUseCase,
            UnsubscribeBoardUseCase unsubscribeBoardUseCase
    ) {
        this.createBoardUseCase = createBoardUseCase;
        this.getBoardQuery = getBoardQuery;
        this.searchBoardQuery = searchBoardQuery;
        this.subscribeBoardUseCase = subscribeBoardUseCase;
        this.unsubscribeBoardUseCase = unsubscribeBoardUseCase;
    }

    @Operation(summary = "게시판 생성")
    @PostMapping
    public ResponseEntity<BoardResult> create(
            @Valid @RequestBody CreateBoardRequest request,
            @AuthenticationPrincipal Long memberId
    ) {
        BoardResult result = createBoardUseCase.create(
                new CreateBoardCommand(
                        request.name(),
                        request.description(),
                        request.category(),
                        request.scope(),
                        memberId,
                        request.universityId()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "게시판 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<BoardResult> getById(@PathVariable Long id) {
        BoardResult result = getBoardQuery.getById(new BoardId(id));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시판 검색")
    @GetMapping
    public ResponseEntity<BoardPageResult> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        BoardPageResult result = searchBoardQuery.search(
                new SearchBoardCommand(keyword, category, scope, page, size)
        );
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시판 구독")
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Void> subscribe(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        subscribeBoardUseCase.subscribe(new SubscribeBoardCommand(id, memberId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시판 구독 취소")
    @DeleteMapping("/{id}/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable Long id,
            @AuthenticationPrincipal Long memberId
    ) {
        unsubscribeBoardUseCase.unsubscribe(new SubscribeBoardCommand(id, memberId));
        return ResponseEntity.noContent().build();
    }
}
