package me.yeonjae.ovlo.application.service.admin;

import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminBoardResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminPostResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminStatsResponse;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminUniversityResponse;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final SearchBoardPort searchBoardPort;
    private final LoadPostPort loadPostPort;
    private final SavePostPort savePostPort;
    private final SearchUniversityPort searchUniversityPort;

    public AdminService(LoadMemberPort loadMemberPort,
                        SaveMemberPort saveMemberPort,
                        SearchBoardPort searchBoardPort,
                        LoadPostPort loadPostPort,
                        SavePostPort savePostPort,
                        SearchUniversityPort searchUniversityPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.searchBoardPort = searchBoardPort;
        this.loadPostPort = loadPostPort;
        this.savePostPort = savePostPort;
        this.searchUniversityPort = searchUniversityPort;
    }

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return loadMemberPort.findAll(pageable).map(AdminMemberResponse::of);
    }

    @Transactional
    public AdminMemberResponse updateMemberRole(Long memberId, MemberRole role) {
        Member member = findMemberOrThrow(memberId);
        member.updateRole(role);
        return AdminMemberResponse.of(saveMemberPort.save(member));
    }

    @Transactional
    public void suspendMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        member.suspend();
        saveMemberPort.save(member);
    }

    @Transactional
    public void unsuspendMember(Long memberId) {
        Member member = findMemberOrThrow(memberId);
        member.unsuspend();
        saveMemberPort.save(member);
    }

    @Transactional
    public void deletePost(Long postId) {
        // MEDIUM-3 fix: PostException import로 완전 클래스명 제거
        Post post = loadPostPort.findById(new PostId(postId))
                .orElseThrow(() -> new PostException("게시글을 찾을 수 없습니다", PostException.ErrorType.NOT_FOUND));
        post.delete();
        savePostPort.save(post);
    }

    public Page<AdminBoardResponse> getBoards(Pageable pageable) {
        // HIGH-4 fix: int 캐스팅 제거 — long 유지
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        var boards = searchBoardPort.search("", null, null, (int) Math.min(offset, Integer.MAX_VALUE), limit);
        long total = searchBoardPort.count("", null, null);
        return new PageImpl<>(boards.stream().map(AdminBoardResponse::of).toList(), pageable, total);
    }

    public Page<AdminPostResponse> getPosts(Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        var posts = loadPostPort.findAll((int) Math.min(offset, Integer.MAX_VALUE), limit);
        long total = loadPostPort.count();
        return new PageImpl<>(posts.stream().map(AdminPostResponse::of).toList(), pageable, total);
    }

    /** 관리자 인증 발급 폼의 대학 선택용 — 키워드 검색(최대 limit건). */
    public List<AdminUniversityResponse> searchUniversities(String keyword, int limit) {
        String kw = keyword == null ? "" : keyword.trim();
        return searchUniversityPort.search(kw, null, 0, limit).stream()
                .map(AdminUniversityResponse::of)
                .toList();
    }

    public Page<AdminUniversityResponse> getUniversities(Pageable pageable) {
        long offset = pageable.getOffset();
        int limit = pageable.getPageSize();
        var universities = searchUniversityPort.search("", null, (int) Math.min(offset, Integer.MAX_VALUE), limit);
        long total = searchUniversityPort.count("", null);
        return new PageImpl<>(universities.stream().map(AdminUniversityResponse::of).toList(), pageable, total);
    }

    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                loadMemberPort.count(),
                searchBoardPort.count("", null, null),
                loadPostPort.count(),
                searchUniversityPort.count("", null)
        );
    }

    private Member findMemberOrThrow(Long memberId) {
        return loadMemberPort.findById(new MemberId(memberId))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다", MemberException.ErrorType.NOT_FOUND));
    }
}
