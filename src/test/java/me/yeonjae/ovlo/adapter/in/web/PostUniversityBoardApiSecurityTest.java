package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.board.SaveBoardPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.application.port.out.post.SavePostPort;
import me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort;
import me.yeonjae.ovlo.domain.board.model.Board;
import me.yeonjae.ovlo.domain.board.model.BoardCategory;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.board.model.LocationScope;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * UNIVERSITY 범위 게시판의 글·댓글 작성이 "그 대학의 활성 인증 자격을 가진 회원"에게만 열리도록
 * 실제 {@code securityFilterChain} + 실제 서비스·영속(H2)을 태워 고정한다.
 *
 * <p>이전에는 게시판 작성 게이트가 {@code minTrustLevel} 비교뿐이어서, 등급만 맞으면 어느 대학
 * 게시판에나 글을 쓸 수 있었다. 회원 프로필의 본교({@code homeUniversityId})는 가입·온보딩 때 본인이
 * 고른 값이라 소속 근거로 치지 않는다 — 여기서는 "본교가 그 대학이지만 인증은 없는 회원"이 403 을
 * 받는지까지 고정한다(ADR-0005).
 *
 * <p>standalone MockMvc 컨트롤러 테스트는 보안 필터와 서비스를 건너뛰므로 이 회귀를 잡지 못한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostUniversityBoardApiSecurityTest {

    private static final long BOARD_UNIVERSITY = 7_001L;
    private static final long OTHER_UNIVERSITY = 7_002L;

    @Autowired private WebApplicationContext wac;
    @Autowired private SaveMemberPort saveMemberPort;
    @Autowired private SaveBoardPort saveBoardPort;
    @Autowired private SavePostPort savePostPort;
    @Autowired private LoadPostPort loadPostPort;
    @Autowired private SaveVerificationCredentialPort saveVerificationCredentialPort;

    private MockMvc mockMvc;
    private Long boardId;
    private Long verifiedMemberId;      // 게시판 대학 인증 보유
    private Long homeOnlyMemberId;      // 본교만 게시판 대학, 인증 없음
    private Long otherVerifiedMemberId; // 다른 대학 인증만 보유
    private Long expiredMemberId;       // 게시판 대학 인증이 있었으나 만료됨
    private Long adminVerifiedMemberId; // 관리자 수동 인증(ADMIN_VERIFIED)으로 게시판 대학 소속

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();

        Member creator = newMember("creator", OTHER_UNIVERSITY);
        boardId = saveBoardPort.save(Board.create("대학 게시판", "소속 인증 회원 전용",
                BoardCategory.GENERAL, LocationScope.UNIVERSITY, creator.getId(),
                new UniversityId(BOARD_UNIVERSITY))).getId().value();

        verifiedMemberId = newMember("verified", OTHER_UNIVERSITY).getId().value();
        saveVerificationCredentialPort.save(VerificationCredential.issue(
                verifiedMemberId, VerificationType.SCHOOL_EMAIL, BOARD_UNIVERSITY,
                "verified@board-univ.edu", Instant.now()));

        homeOnlyMemberId = newMember("homeonly", BOARD_UNIVERSITY).getId().value();

        otherVerifiedMemberId = newMember("other", BOARD_UNIVERSITY).getId().value();
        saveVerificationCredentialPort.save(VerificationCredential.issue(
                otherVerifiedMemberId, VerificationType.SCHOOL_EMAIL, OTHER_UNIVERSITY,
                "other@other-univ.edu", Instant.now()));

        expiredMemberId = newMember("expired", BOARD_UNIVERSITY).getId().value();
        VerificationCredential expired = VerificationCredential.issue(
                expiredMemberId, VerificationType.SCHOOL_EMAIL, BOARD_UNIVERSITY,
                "expired@board-univ.edu", Instant.now());
        expired.expire();
        saveVerificationCredentialPort.save(expired);

        adminVerifiedMemberId = newMember("admin-verified", OTHER_UNIVERSITY).getId().value();
        saveVerificationCredentialPort.save(VerificationCredential.issueManual(
                adminVerifiedMemberId, BOARD_UNIVERSITY, null, "admin@test.example.com", "서류 확인", Instant.now()));
    }

    @Test
    @DisplayName("비로그인 글 작성은 401")
    void createPost_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("게시판 대학의 활성 인증을 가진 회원은 글을 쓸 수 있다 (201)")
    void createPost_verifiedAtBoardUniversity_isCreated() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(loginAs(verifiedMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boardId").value(boardId));
    }

    @Test
    @DisplayName("관리자 수동 인증(ADMIN_VERIFIED)도 소속으로 인정한다 (201)")
    void createPost_adminVerifiedAtBoardUniversity_isCreated() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(loginAs(adminVerifiedMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("게시판 대학 인증이 만료된 회원은 403 — 활성 자격만 소속으로 친다")
    void createPost_expiredCredential_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(loginAs(expiredMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    @DisplayName("본교만 그 대학이고 인증이 없는 회원은 403 — homeUniversityId 는 소속 근거가 아니다")
    void createPost_homeUniversityOnly_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(loginAs(homeOnlyMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    @DisplayName("다른 대학 인증만 가진 회원은 403")
    void createPost_verifiedAtOtherUniversity_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .with(authentication(loginAs(otherVerifiedMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    @DisplayName("대학 게시판 글에 소속 인증이 없는 회원이 댓글을 달면 403 이고 댓글은 저장되지 않는다")
    void createComment_nonMember_isForbidden() throws Exception {
        Long postId = seedPostByVerifiedMember();

        mockMvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(authentication(loginAs(otherVerifiedMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));

        assertThat(loadPostPort.findById(new PostId(postId)).orElseThrow().getComments()).isEmpty();
    }

    @Test
    @DisplayName("대학 게시판 글에 인증이 만료된 회원이 댓글을 달면 403")
    void createComment_expiredCredential_isForbidden() throws Exception {
        Long postId = seedPostByVerifiedMember();

        mockMvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(authentication(loginAs(expiredMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
    }

    @Test
    @DisplayName("대학 게시판 글에 소속 인증 회원은 댓글을 달 수 있다 (201)")
    void createComment_member_isCreated() throws Exception {
        Long postId = seedPostByVerifiedMember();

        mockMvc.perform(post("/api/v1/posts/{id}/comments", postId)
                        .with(authentication(loginAs(verifiedMemberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("댓글"));
    }

    private Long seedPostByVerifiedMember() {
        Post post = Post.create(new BoardId(boardId), new MemberId(verifiedMemberId), "제목", "내용");
        return savePostPort.save(post).getId().value();
    }

    private String postBody() {
        return "{\"boardId\":" + boardId + ",\"title\":\"제목\",\"content\":\"내용\"}";
    }

    private static Authentication loginAs(Long memberId) {
        return new UsernamePasswordAuthenticationToken(memberId, null, List.of());
    }

    private Member newMember(String tag, long homeUniversityId) {
        return saveMemberPort.save(Member.create(
                "ub-" + tag, "테스터", "Seoul",
                new Email("ub-" + tag + "@example.com"),
                new Password("hashedPassword"),
                new UniversityId(homeUniversityId),
                new Major("Computer Science", DegreeType.BACHELOR, 3)));
    }
}
