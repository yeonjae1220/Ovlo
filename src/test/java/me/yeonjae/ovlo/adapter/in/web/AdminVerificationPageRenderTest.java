package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.adapter.out.persistence.MemberPersistenceAdapter;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * /admin/verifications Thymeleaf 렌더링 검증.
 * 템플릿 표현식 오류는 컴파일이 아닌 렌더 시점에만 드러나므로 실제 ThymeleafViewResolver로 세 상태를 렌더한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(authorities = "ADMIN")
class AdminVerificationPageRenderTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private MemberPersistenceAdapter memberAdapter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("회원 미지정 — 검색 화면만 렌더")
    void rendersSearchOnly() throws Exception {
        mockMvc.perform(get("/admin/verifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/verifications"))
                .andExpect(content().string(containsString("회원 검색")));
    }

    @Test
    @DisplayName("검색어 입력 — 결과 목록 섹션 렌더(빈 결과)")
    void rendersSearchResults() throws Exception {
        mockMvc.perform(get("/admin/verifications").param("q", "없는닉네임zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("일치하는 회원이 없습니다")));
    }

    @Test
    @DisplayName("회원 지정 — 컨텍스트 헤더 + 발급 폼 + 자동완성 스크립트 렌더")
    @Transactional
    void rendersMemberContext() throws Exception {
        Member m = memberAdapter.save(Member.create(
                "renderTester", "렌더", "Seoul",
                new Email("render-tester@example.com"), new Password("hashedPassword"),
                new UniversityId(1L), new Major("CS", DegreeType.BACHELOR, 3)));
        long id = m.getId().value();

        mockMvc.perform(get("/admin/verifications").param("memberId", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/verifications"))
                .andExpect(content().string(containsString("현재 신뢰 등급")))
                .andExpect(content().string(containsString("renderTester")))
                .andExpect(content().string(containsString("verification-autocomplete.js")));
    }
}
