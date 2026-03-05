package me.yeonjae.ovlo.domain.board.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;

import java.util.Objects;

public class BoardSubscription {

    private BoardId boardId;
    private MemberId memberId;

    private BoardSubscription() {}

    public static BoardSubscription create(BoardId boardId, MemberId memberId) {
        Objects.requireNonNull(boardId, "게시판 ID는 필수입니다");
        Objects.requireNonNull(memberId, "회원 ID는 필수입니다");

        BoardSubscription subscription = new BoardSubscription();
        subscription.boardId = boardId;
        subscription.memberId = memberId;
        return subscription;
    }

    public BoardId getBoardId() { return boardId; }
    public MemberId getMemberId() { return memberId; }
}
