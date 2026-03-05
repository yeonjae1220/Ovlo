package me.yeonjae.ovlo.domain.board.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;

import java.util.Objects;

public class Board {

    private BoardId id;
    private String name;
    private String description;
    private BoardCategory category;
    private LocationScope scope;
    private MemberId creatorId;
    private UniversityId universityId; // scope == UNIVERSITY일 때만 값 존재
    private boolean active;

    private Board() {}

    public static Board create(
            String name,
            String description,
            BoardCategory category,
            LocationScope scope,
            MemberId creatorId,
            UniversityId universityId) {

        Objects.requireNonNull(name, "게시판 이름은 필수입니다");
        if (name.isBlank()) throw new IllegalArgumentException("게시판 이름은 빈 값일 수 없습니다");
        Objects.requireNonNull(category, "게시판 카테고리는 필수입니다");
        Objects.requireNonNull(scope, "게시판 범위는 필수입니다");
        Objects.requireNonNull(creatorId, "게시판 생성자 ID는 필수입니다");

        if (scope == LocationScope.UNIVERSITY && universityId == null) {
            throw new IllegalArgumentException("UNIVERSITY 범위 게시판은 대학 ID가 필수입니다");
        }

        Board board = new Board();
        board.name = name;
        board.description = description;
        board.category = category;
        board.scope = scope;
        board.creatorId = creatorId;
        board.universityId = universityId;
        board.active = true;
        return board;
    }

    /** persistence 계층 전용: DB에서 모든 필드를 복원할 때 사용 */
    public static Board restore(
            BoardId id,
            String name,
            String description,
            BoardCategory category,
            LocationScope scope,
            MemberId creatorId,
            UniversityId universityId,
            boolean active) {

        Board board = new Board();
        board.id = id;
        board.name = name;
        board.description = description;
        board.category = category;
        board.scope = scope;
        board.creatorId = creatorId;
        board.universityId = universityId;
        board.active = active;
        return board;
    }

    public void deactivate() {
        if (!active) {
            throw new IllegalStateException("이미 비활성화된 게시판입니다");
        }
        this.active = false;
    }

    public BoardId getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BoardCategory getCategory() { return category; }
    public LocationScope getScope() { return scope; }
    public MemberId getCreatorId() { return creatorId; }
    public UniversityId getUniversityId() { return universityId; }
    public boolean isActive() { return active; }
}
