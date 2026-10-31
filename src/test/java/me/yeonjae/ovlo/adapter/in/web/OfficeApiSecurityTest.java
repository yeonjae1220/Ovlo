package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStatus;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 국제처 테넌트 경계를 실제 {@code securityFilterChain} + JWT 필터로 고정한다(ADR-0005, GLOBAL-PIT-189).
 * 토큰은 실제 {@link JwtTokenProvider} 로 발급해 클레임 → 권한 → 정책까지 한 경로로 검증한다.
 *
 * <ul>
 *   <li>클레임 없는 일반 회원: office-admin 403</li>
 *   <li>다른 Office 담당자: 403 (1차 관문은 통과하지만 정책이 막는다)</li>
 *   <li>같은 Office STAFF: 조회 200, 초대(ADMIN 전용) 403</li>
 *   <li>비로그인: office/me 401, admin/offices 401 · 일반 회원의 admin/offices 403</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class OfficeApiSecurityTest {

    private static final long OFFICE_ID = 5L;
    private static final long OTHER_OFFICE_ID = 7L;
    private static final long MEMBER_ID = 42L;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private ManageOfficeStaffUseCase manageStaffUseCase;
    @MockitoBean private AdministerOfficeUseCase administerUseCase;
    @MockitoBean private AcceptOfficeInvitationUseCase acceptUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
        given(getOfficeQuery.findById(OFFICE_ID)).willReturn(Optional.of(
                new OfficeResult(OFFICE_ID, 266L, "성균관대학교", "국제처", OfficeStatus.PILOT, Instant.now(), List.of())));
    }

    private String bearer(OfficeMembership membership) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, membership);
    }

    @Test
    @DisplayName("담당자 클레임이 없는 일반 회원은 office-admin 에 403 이고 조회에 도달하지 않는다")
    void plainMember_officeAdmin_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID).header("Authorization", bearer(null)))
                .andExpect(status().isForbidden());

        verify(getOfficeQuery, never()).findById(anyLong());
    }

    @Test
    @DisplayName("다른 Office 의 관리자는 1차 관문은 통과하지만 범위 정책에서 403 OFFICE_FORBIDDEN")
    void otherOfficeAdmin_forbiddenByPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID)
                        .header("Authorization", bearer(new OfficeMembership(OTHER_OFFICE_ID, OfficeRole.OFFICE_ADMIN))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OFFICE_FORBIDDEN"));

        verify(getOfficeQuery, never()).findById(anyLong());
    }

    @Test
    @DisplayName("같은 Office 의 STAFF 는 조회는 되지만 초대(ADMIN 전용)는 403")
    void sameOfficeStaff_canReadButCannotInvite() throws Exception {
        String token = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OFFICE_ID));

        mockMvc.perform(post("/api/v1/office-admin/" + OFFICE_ID + "/staff")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"s@univ.ac.kr\",\"role\":\"OFFICE_STAFF\"}"))
                .andExpect(status().isForbidden());

        verify(manageStaffUseCase, never()).invite(any());
    }

    @Test
    @DisplayName("같은 Office 의 ADMIN 은 초대·제거를 할 수 있다")
    void sameOfficeAdmin_canManageStaff() throws Exception {
        String token = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_ADMIN));

        mockMvc.perform(post("/api/v1/office-admin/" + OFFICE_ID + "/staff")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"s@univ.ac.kr\",\"role\":\"OFFICE_STAFF\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/office-admin/" + OFFICE_ID + "/staff/99").header("Authorization", token))
                .andExpect(status().isNoContent());

        verify(manageStaffUseCase).invite(any());
        verify(manageStaffUseCase).remove(any());
    }

    @Test
    @DisplayName("비로그인은 /office/me 401, 일반 회원은 /admin/offices 403, 비로그인 /admin/offices 401")
    void adminAndMeEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/office/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/offices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/offices").header("Authorization", bearer(null)))
                .andExpect(status().isForbidden());
        // 담당자 클레임이 있어도 플랫폼 ADMIN 은 아니다
        mockMvc.perform(get("/api/v1/admin/offices")
                        .header("Authorization", bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_ADMIN))))
                .andExpect(status().isForbidden());

        verify(administerUseCase, never()).create(any());
    }
}
