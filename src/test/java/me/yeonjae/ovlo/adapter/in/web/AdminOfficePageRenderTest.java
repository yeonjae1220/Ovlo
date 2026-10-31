package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** /admin/offices Thymeleaf 렌더링과 폼 위임. 템플릿 표현식 오류는 렌더 시점에만 드러난다. */
@SpringBootTest
@ActiveProfiles("test")
class AdminOfficePageRenderTest {

    @Autowired private WebApplicationContext wac;

    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private AdministerOfficeUseCase administerOfficeUseCase;
    @MockitoBean private ManageOfficeStaffUseCase manageOfficeStaffUseCase;
    /** OfficeCommandService 가 세 인터페이스를 한 빈으로 구현하므로, 일부만 mock 하면 나머지 인터페이스의 빈이 사라진다. */
    @MockitoBean private AcceptOfficeInvitationUseCase acceptOfficeInvitationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("목록 — 담당자가 있는 Office 와 없는 Office 를 함께 렌더한다")
    void rendersList() throws Exception {
        var withStaff = new OfficeResult(5L, 266L, "성균관대학교", "성균관대 국제처", OfficeStatus.PILOT, Instant.now(),
                List.of(new OfficeStaffResult(11L, "staff@skku.edu", "staff", OfficeRole.OFFICE_ADMIN,
                        OfficeStaffStatus.ACTIVE, Instant.now(), Instant.now())));
        var empty = new OfficeResult(6L, 70L, "TUM", "TUM International", OfficeStatus.SUSPENDED, Instant.now(), List.of());
        given(getOfficeQuery.findAll(any())).willReturn(new PageImpl<>(List.of(withStaff, empty)));

        mockMvc.perform(get("/admin/offices"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/offices"))
                .andExpect(content().string(containsString("성균관대 국제처")))
                .andExpect(content().string(containsString("staff@skku.edu")))
                .andExpect(content().string(containsString("담당자 없음")))
                .andExpect(content().string(containsString("재개")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("생성 폼 — 유스케이스에 위임하고 목록으로 리다이렉트")
    void createDelegates() throws Exception {
        given(administerOfficeUseCase.create(any())).willReturn(
                new OfficeResult(9L, 266L, "성균관대학교", "국제처", OfficeStatus.PILOT, Instant.now(), List.of()));

        mockMvc.perform(post("/admin/offices").with(csrf())
                        .param("globalUnivId", "266").param("name", "국제처"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/offices"));

        verify(administerOfficeUseCase).create(any());
    }

    @Test
    @DisplayName("비로그인은 로그인 페이지로 보내고 유스케이스에 닿지 않는다")
    void anonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/offices"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        verify(getOfficeQuery, never()).findAll(any());
    }
}
