package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.adapter.out.persistence.MemberPersistenceAdapter;
import me.yeonjae.ovlo.application.dto.command.AdminWithdrawMemberCommand;
import me.yeonjae.ovlo.application.port.in.member.AdminWithdrawMemberUseCase;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 관리자 수동 탈퇴(Apple 재인증 불가 회원의 대체 경로)를 실제 admin 보안 체인으로 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
class AdminMemberWithdrawPageTest {

    @Autowired WebApplicationContext context;
    @Autowired MemberPersistenceAdapter memberAdapter;
    @MockitoBean AdminWithdrawMemberUseCase adminWithdrawMemberUseCase;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "ops-admin", authorities = "ADMIN")
    @DisplayName("관리자가 사유와 함께 탈퇴 처리하면 관리자 이름·사유가 명령에 실린다")
    void admin_withdrawsWithReason() throws Exception {
        mvc.perform(post("/admin/members/42/withdraw").with(csrf()).param("reason", "Apple ID 분실, 문의 #12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/members"))
                .andExpect(flash().attributeExists("successMessage"));

        then(adminWithdrawMemberUseCase).should()
                .withdrawByAdmin(new AdminWithdrawMemberCommand(42L, "Apple ID 분실, 문의 #12", "ops-admin"));
    }

    @Test
    @WithMockUser(username = "ops-admin", authorities = "ADMIN")
    @DisplayName("거부된 요청(사유 없음·관리자 대상 등)은 오류 메시지로 돌려보낸다")
    void rejected_showsError() throws Exception {
        willThrow(new MemberException("관리자 계정은 이 화면에서 탈퇴시킬 수 없습니다",
                MemberException.ErrorType.FORBIDDEN)).given(adminWithdrawMemberUseCase).withdrawByAdmin(any());

        mvc.perform(post("/admin/members/42/withdraw").with(csrf()).param("reason", "사유"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", containsString("관리자 계정")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("CSRF 토큰 없는 요청은 막힌다")
    void withoutCsrf_forbidden() throws Exception {
        mvc.perform(post("/admin/members/42/withdraw").param("reason", "사유"))
                .andExpect(status().isForbidden());

        then(adminWithdrawMemberUseCase).should(never()).withdrawByAdmin(any());
    }

    @Test
    @WithMockUser(authorities = "MEMBER")
    @DisplayName("관리자가 아니면 탈퇴 처리를 할 수 없다")
    void nonAdmin_cannotWithdraw() throws Exception {
        mvc.perform(post("/admin/members/42/withdraw").with(csrf()).param("reason", "사유"))
                .andExpect(status().isForbidden());

        then(adminWithdrawMemberUseCase).should(never()).withdrawByAdmin(any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("회원 목록 화면에 사유 입력이 필수인 탈퇴 처리 폼이 렌더된다")
    @Transactional
    void membersPage_rendersWithdrawForm() throws Exception {
        long id = memberAdapter.save(Member.create("withdrawRender", "렌더", "Seoul",
                new Email("withdraw-render@example.com"), new Password("hashedPassword"),
                new UniversityId(1L), new Major("CS", DegreeType.BACHELOR, 3))).getId().value();

        mvc.perform(get("/admin/members"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/admin/members/" + id + "/withdraw")))
                .andExpect(content().string(containsString("name=\"reason\" required")));
    }
}
