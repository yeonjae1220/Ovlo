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
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final SearchBoardPort searchBoardPort;
    private final LoadPostPort loadPostPort;
    private final SearchUniversityPort searchUniversityPort;

    public AdminService(LoadMemberPort loadMemberPort,
                        SaveMemberPort saveMemberPort,
                        SearchBoardPort searchBoardPort,
                        LoadPostPort loadPostPort,
                        SearchUniversityPort searchUniversityPort) {
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.searchBoardPort = searchBoardPort;
        this.loadPostPort = loadPostPort;
        this.searchUniversityPort = searchUniversityPort;
    }

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return loadMemberPort.findAll(pageable)
                .map(AdminMemberResponse::of);
    }

    @Transactional
    public AdminMemberResponse updateMemberRole(Long memberId, MemberRole role) {
        Member member = loadMemberPort.findById(new MemberId(memberId))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다", MemberException.ErrorType.NOT_FOUND));
        member.updateRole(role);
        Member saved = saveMemberPort.save(member);
        return AdminMemberResponse.of(saved);
    }

    public Page<AdminBoardResponse> getBoards(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        var boards = searchBoardPort.search("", null, null, offset, limit);
        long total = searchBoardPort.count("", null, null);
        return new PageImpl<>(boards.stream().map(AdminBoardResponse::of).toList(), pageable, total);
    }

    public Page<AdminPostResponse> getPosts(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        var posts = loadPostPort.findAll(offset, limit);
        long total = loadPostPort.count();
        return new PageImpl<>(posts.stream().map(AdminPostResponse::of).toList(), pageable, total);
    }

    public Page<AdminUniversityResponse> getUniversities(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit = pageable.getPageSize();
        var universities = searchUniversityPort.search("", null, offset, limit);
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
}
