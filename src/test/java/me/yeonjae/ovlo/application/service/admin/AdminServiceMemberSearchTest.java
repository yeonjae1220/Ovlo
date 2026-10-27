package me.yeonjae.ovlo.application.service.admin;

import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.application.port.out.board.SearchBoardPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminServiceMemberSearchTest {

    @Mock private LoadMemberPort loadMemberPort;
    @Mock private SaveMemberPort saveMemberPort;
    @Mock private SearchBoardPort searchBoardPort;
    @Mock private LoadPostPort loadPostPort;
    @Mock private SavePostPort savePostPort;
    @Mock private SearchUniversityPort searchUniversityPort;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(loadMemberPort, saveMemberPort, searchBoardPort,
                loadPostPort, savePostPort, searchUniversityPort);
    }

    @Test
    @DisplayName("이메일 일치 회원을 맨 앞에 두고 닉네임 일치를 중복 없이 붙이며, 닉네임 조회에도 상한을 건다")
    void searchMembers_putsEmailMatchFirst_dedupes_andLimitsNicknameQuery() {
        Member emailMatch = member(1L, "kim", "kim@example.com");
        Member nicknameMatch = member(2L, "kimchi", "chi@example.com");
        given(loadMemberPort.findByEmail("kim")).willReturn(Optional.of(emailMatch));
        given(loadMemberPort.searchByNickname("kim", 2)).willReturn(List.of(nicknameMatch, emailMatch));

        List<AdminMemberResponse> results = adminService.searchMembers(" kim ", 2);

        assertThat(results).extracting(AdminMemberResponse::id).containsExactly(1L, 2L);
    }

    private Member member(long id, String nickname, String email) {
        Member member = Member.create(
                nickname, "관리대상", "Seoul",
                new Email(email),
                new Password("hashedPassword"),
                new UniversityId(1L),
                new Major("Computer Science", DegreeType.BACHELOR, 3));
        member.assignId(new MemberId(id));
        return member;
    }
}
