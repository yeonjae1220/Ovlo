package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.PartnerResult;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import me.yeonjae.ovlo.application.port.in.office.StudentPartnerUseCase;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
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

/** /admin/partners Thymeleaf 렌더링과 폼 위임. */
@SpringBootTest
@ActiveProfiles("test")
class AdminPartnerPageRenderTest {

    @Autowired private WebApplicationContext wac;

    @MockitoBean private ManagePartnerUseCase managePartnerUseCase;
    /** PartnerService 가 두 인터페이스를 한 빈으로 구현한다. */
    @MockitoBean private StudentPartnerUseCase studentPartnerUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("목록 — 켜진/꺼진 제휴를 함께 렌더하고 제휴 표시 문구를 안내한다")
    void rendersList() throws Exception {
        given(managePartnerUseCase.findAll()).willReturn(List.of(
                new PartnerResult(1L, PartnerCategory.SIM, "KT 유심", "입국 전 예약", "https://kt.example", true, 0, Instant.now()),
                new PartnerResult(2L, PartnerCategory.PICKUP, "공항 픽업", null, "https://pickup.example", false, 1, Instant.now())));

        mockMvc.perform(get("/admin/partners"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/partners"))
                .andExpect(content().string(containsString("KT 유심")))
                .andExpect(content().string(containsString("공항 픽업")))
                .andExpect(content().string(containsString("끄기")))
                .andExpect(content().string(containsString("켜기")))
                .andExpect(content().string(containsString("수수료를 받을 수 있습니다")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("등록 폼은 유스케이스에 위임하고 목록으로 돌아간다")
    void createDelegates() throws Exception {
        given(managePartnerUseCase.create(any())).willReturn(
                new PartnerResult(3L, PartnerCategory.INSURANCE, "DB손보", null, "https://db.example", false, 0, Instant.now()));

        mockMvc.perform(post("/admin/partners").with(csrf())
                        .param("category", "INSURANCE").param("name", "DB손보").param("targetUrl", "https://db.example"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/partners"));

        verify(managePartnerUseCase).create(new UpsertPartnerCommand(PartnerCategory.INSURANCE, "DB손보", null, "https://db.example", 0));
    }

    @Test
    @DisplayName("로그인 없이는 관리자 로그인으로 보낸다")
    void anonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/partners"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
        verify(managePartnerUseCase, never()).findAll();
    }
}
