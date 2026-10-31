package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.verification.LoadVerificationCredentialPort;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeId;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.verification.model.VerificationCredential;
import me.yeonjae.ovlo.domain.verification.model.VerificationType;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 승인 자료 API 의 테넌트·소속 경계를 실제 필터 체인으로 고정한다.
 * - 담당자: 다른 Office 공지 403, 같은 Office 는 등록 201, 본문 검증 실패는 400(401 로 위장되지 않음, GLOBAL-PIT-156)
 * - 학생: 그 대학 활성 인증 자격이 없으면 403, 있으면 200
 */
@SpringBootTest
@ActiveProfiles("test")
class OfficeNoticeApiSecurityTest {

    private static final long OFFICE_ID = 5L;
    private static final long UNIV = 266L;
    private static final long MEMBER_ID = 42L;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private ManageNoticeUseCase manageNoticeUseCase;
    @MockitoBean private GetNoticeQuery getNoticeQuery;
    // 영속 어댑터가 여러 포트를 한 빈으로 구현하므로 같은 어댑터의 나머지 포트도 함께 mock 한다
    @MockitoBean private LoadOfficePort loadOfficePort;
    @MockitoBean private me.yeonjae.ovlo.application.port.out.office.SaveOfficePort saveOfficePort;
    @MockitoBean private me.yeonjae.ovlo.application.port.out.office.LoadOfficeMembershipPort loadOfficeMembershipPort;
    @MockitoBean private LoadVerificationCredentialPort loadCredentialPort;
    @MockitoBean private me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort saveCredentialPort;
    @MockitoBean private me.yeonjae.ovlo.application.port.out.verification.ExpireVerificationCredentialPort expireCredentialPort;
    // OfficeCommandService 의 세 인터페이스와 GetOfficeQuery 는 함께 mock 해야 컨텍스트가 뜬다
    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private ManageOfficeStaffUseCase manageOfficeStaffUseCase;
    @MockitoBean private AdministerOfficeUseCase administerOfficeUseCase;
    @MockitoBean private AcceptOfficeInvitationUseCase acceptOfficeInvitationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
        Office office = Office.create(UNIV, "국제처", Instant.now());
        office.assignId(new OfficeId(OFFICE_ID));
        given(loadOfficePort.findById(OFFICE_ID)).willReturn(Optional.of(office));
        given(getNoticeQuery.findForStaff(eq(OFFICE_ID), any(), any(), any())).willReturn(new PageImpl<>(List.of()));
        given(getNoticeQuery.findForStudent(eq(OFFICE_ID), any(), any())).willReturn(List.of());
    }

    private String bearer(OfficeMembership membership) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, membership);
    }

    @Test
    @DisplayName("다른 Office 담당자의 공지 목록은 403 이고 조회에 닿지 않는다")
    void staff_otherOffice_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/notices")
                        .header("Authorization", bearer(new OfficeMembership(7L, OfficeRole.OFFICE_STAFF))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OFFICE_FORBIDDEN"));

        verify(getNoticeQuery, never()).findForStaff(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("같은 Office 담당자는 목록 200, 등록 201 이고 본문 검증 실패는 400 이다")
    void staff_sameOffice_listAndRegister() throws Exception {
        String token = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/notices").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/office-admin/" + OFFICE_ID + "/notices")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"HOUSING","title":"기숙사","body":"2월 10일까지","term":"2027-1",
                                 "effectiveFrom":"2027-02-01","expiresAt":"2027-03-15"}
                                """))
                .andExpect(status().isCreated());
        verify(manageNoticeUseCase).register(any());

        mockMvc.perform(post("/api/v1/office-admin/" + OFFICE_ID + "/notices")
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"HOUSING\",\"title\":\"\",\"body\":\"x\",\"term\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("학생: 그 대학 활성 인증 자격이 없으면 403, 있으면 200")
    void student_requiresVerifiedCredential() throws Exception {
        String token = bearer(null);
        given(loadCredentialPort.findByMemberId(MEMBER_ID)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/notices").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OFFICE_FORBIDDEN"));
        verify(getNoticeQuery, never()).findForStudent(anyLong(), any(), any());

        given(loadCredentialPort.findByMemberId(MEMBER_ID)).willReturn(List.of(
                VerificationCredential.issue(MEMBER_ID, VerificationType.SCHOOL_EMAIL, UNIV, "s@univ.ac.kr", Instant.now())));

        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/notices").param("lang", "vi").header("Authorization", token))
                .andExpect(status().isOk());
        verify(getNoticeQuery).findForStudent(eq(OFFICE_ID), any(), any());
    }

    @Test
    @DisplayName("비로그인은 401")
    void anonymous() throws Exception {
        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/notices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/notices")).andExpect(status().isUnauthorized());
    }
}
