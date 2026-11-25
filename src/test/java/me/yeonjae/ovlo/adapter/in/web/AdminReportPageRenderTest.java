package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.ReportSummaryResult;
import me.yeonjae.ovlo.application.port.in.moderation.BlockMemberUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.GetBlockedMembersQuery;
import me.yeonjae.ovlo.application.port.in.moderation.ModerateReportUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.ReportContentUseCase;
import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** /admin/reports Thymeleaf 렌더링과 처리 폼 위임. */
@SpringBootTest
@ActiveProfiles("test")
class AdminReportPageRenderTest {

    @Autowired private WebApplicationContext wac;

    /** ModerationService 가 네 인터페이스를 한 빈으로 구현한다. */
    @MockitoBean private ModerateReportUseCase moderateReportUseCase;
    @MockitoBean private ReportContentUseCase reportContentUseCase;
    @MockitoBean private BlockMemberUseCase blockMemberUseCase;
    @MockitoBean private GetBlockedMembersQuery getBlockedMembersQuery;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    private static ReportSummaryResult report(long id, ReportTargetType type, String preview) {
        return new ReportSummaryResult(id, type, 50L, 2L, "troll", "victim", ReportReason.HARASSMENT,
                "계속 욕설을 보냅니다", ReportStatus.OPEN, null, Instant.parse("2026-11-25T01:00:00Z"), preview, 3);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("처리 대기 신고를 대상 내용·사유와 함께 렌더한다")
    void rendersOpenReports() throws Exception {
        given(moderateReportUseCase.reports(ReportStatus.OPEN)).willReturn(List.of(
                report(1L, ReportTargetType.COMMENT, "욕설 댓글 본문")));

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("욕설 댓글 본문")))
                .andExpect(content().string(containsString("계속 욕설을 보냅니다")))
                .andExpect(content().string(containsString("대기 3건")))
                .andExpect(content().string(containsString("콘텐츠 내리기")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("회원 신고에는 콘텐츠 내리기 버튼이 없다")
    void memberReportHasNoRemoveButton() throws Exception {
        given(moderateReportUseCase.reports(ReportStatus.OPEN)).willReturn(List.of(
                report(1L, ReportTargetType.MEMBER, null)));

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("콘텐츠 내리기"))))
                .andExpect(content().string(containsString("작성자 정지")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ADMIN")
    @DisplayName("처리 폼은 관리자 이름과 함께 유스케이스에 위임한다")
    void resolveDelegates() throws Exception {
        mockMvc.perform(post("/admin/reports/7/resolve").with(csrf()).param("resolution", "REMOVE_CONTENT"))
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(moderateReportUseCase).resolve("admin", 7L, ReportResolution.REMOVE_CONTENT);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("이미 처리된 신고면 오류 메시지를 띄운다")
    void resolveConflictShowsError() throws Exception {
        willThrow(new ModerationException("이미 처리된 신고입니다", ModerationException.ErrorType.CONFLICT))
                .given(moderateReportUseCase).resolve(any(), anyLong(), any());

        mockMvc.perform(post("/admin/reports/7/resolve").with(csrf()).param("resolution", "DISMISS"))
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attribute("errorMessage", "이미 처리된 신고입니다"));
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    @DisplayName("관리자가 아니면 처리할 수 없다")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(post("/admin/reports/7/resolve").with(csrf()).param("resolution", "DISMISS"))
                .andExpect(status().isForbidden());

        verify(moderateReportUseCase, never()).resolve(any(), anyLong(), any());
    }
}
