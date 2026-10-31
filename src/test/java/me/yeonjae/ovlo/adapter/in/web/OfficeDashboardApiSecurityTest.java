package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.OfficeDashboardResult;
import me.yeonjae.ovlo.application.dto.result.PartnerCardResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ExportOfficeDataQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeDashboardQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficePartnerSettingsUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import me.yeonjae.ovlo.application.port.in.office.StudentPartnerUseCase;
import me.yeonjae.ovlo.application.service.office.CsvWriter;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * 대시보드·CSV·제휴 설정·학생 제휴 API 의 경계를 실제 필터 체인으로 고정한다.
 * - 담당자: 다른 Office 403, 같은 Office 200. 설정 변경은 OFFICE_ADMIN 만.
 * - 학생: 소속 검사는 유스케이스(StudentOfficeAccessPolicy)가 하므로 그 예외가 403 으로 나가는지.
 * - 플랫폼 관리자 API 는 ADMIN 권한 없이는 403.
 */
@SpringBootTest
@ActiveProfiles("test")
class OfficeDashboardApiSecurityTest {

    private static final long OFFICE_ID = 5L;
    private static final long MEMBER_ID = 42L;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private GetOfficeDashboardQuery dashboardQuery;
    @MockitoBean private ExportOfficeDataQuery exportQuery;
    @MockitoBean private ManageOfficePartnerSettingsUseCase partnerSettings;
    // PartnerService 는 두 인터페이스를 한 빈으로 구현한다
    @MockitoBean private StudentPartnerUseCase studentPartnerUseCase;
    @MockitoBean private ManagePartnerUseCase managePartnerUseCase;
    // OfficeCommandService 의 세 인터페이스와 GetOfficeQuery 는 함께 mock 해야 컨텍스트가 뜬다
    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private ManageOfficeStaffUseCase manageOfficeStaffUseCase;
    @MockitoBean private AdministerOfficeUseCase administerOfficeUseCase;
    @MockitoBean private AcceptOfficeInvitationUseCase acceptOfficeInvitationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
        OfficeDashboardResult empty = new OfficeDashboardResult(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 2),
                new OfficeDashboardResult.Inquiries(0, 0, 0, 0, null), new OfficeDashboardResult.Feedback(0, 0, null),
                new OfficeDashboardResult.Consultations(0, 0, 0, 0, null, null),
                new OfficeDashboardResult.Notices(0, 0, List.of()), Map.of(), List.of(), List.of());
        given(dashboardQuery.dashboard(eq(OFFICE_ID), any(), any())).willReturn(empty);
        given(exportQuery.inquiriesCsv(eq(OFFICE_ID), any(), any())).willReturn(new CsvWriter().header("id").build());
        given(partnerSettings.enabledCategories(OFFICE_ID)).willReturn(Set.of(PartnerCategory.SIM));
        given(partnerSettings.update(eq(OFFICE_ID), any())).willReturn(Set.of(PartnerCategory.SIM, PartnerCategory.PICKUP));
    }

    private String bearer(OfficeMembership membership) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, membership);
    }

    @Test
    @DisplayName("다른 Office 담당자의 대시보드·CSV 는 403 이고 조회에 닿지 않는다")
    void staff_otherOffice_forbidden() throws Exception {
        String token = bearer(new OfficeMembership(7L, OfficeRole.OFFICE_ADMIN));
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/dashboard").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OFFICE_FORBIDDEN"));
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/export/inquiries.csv").header("Authorization", token))
                .andExpect(status().isForbidden());

        verify(dashboardQuery, never()).dashboard(anyLong(), any(), any());
        verify(exportQuery, never()).inquiriesCsv(anyLong(), any(), any());
    }

    @Test
    @DisplayName("같은 Office 담당자는 대시보드 200, CSV 는 text/csv + BOM + 첨부 헤더")
    void staff_sameOffice_dashboardAndCsv() throws Exception {
        String token = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/dashboard?from=2027-02-01&to=2027-03-02")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inquiries.total").value(0))
                .andExpect(jsonPath("$.inquiries.answerRate").doesNotExist());

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/export/inquiries.csv").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"ovlo-inquiries.csv\""))
                .andExpect(content().string(CsvWriter.BOM + "id\r\n"));
    }

    @Test
    @DisplayName("제휴 설정 조회는 담당자도 되지만 변경은 OFFICE_ADMIN 만 (STAFF 는 403)")
    void partnerSettings_adminOnlyForUpdate() throws Exception {
        String staff = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));
        String admin = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_ADMIN));
        String body = "{\"enabledCategories\":[\"SIM\",\"PICKUP\"]}";

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/settings/partners").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabledCategories[0]").value("SIM"));

        mockMvc.perform(put("/api/v1/office-admin/" + OFFICE_ID + "/settings/partners")
                        .header("Authorization", staff).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(partnerSettings, never()).update(anyLong(), any());

        mockMvc.perform(put("/api/v1/office-admin/" + OFFICE_ID + "/settings/partners")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabledCategories.length()").value(2));

        mockMvc.perform(put("/api/v1/office-admin/" + OFFICE_ID + "/settings/partners")
                        .header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("학생 제휴: 소속이 아니면 유스케이스의 FORBIDDEN 이 403 으로, 소속이면 카드 목록과 클릭 URL")
    void studentPartners() throws Exception {
        String token = bearer(null);
        given(studentPartnerUseCase.forStudent(8L, MEMBER_ID))
                .willThrow(new OfficeException("소속 아님", OfficeException.ErrorType.FORBIDDEN));
        given(studentPartnerUseCase.forStudent(OFFICE_ID, MEMBER_ID))
                .willReturn(List.of(new PartnerCardResult(1L, PartnerCategory.SIM, "KT", null)));
        given(studentPartnerUseCase.click(OFFICE_ID, 1L, MEMBER_ID)).willReturn("https://kt.example/ovlo");

        mockMvc.perform(get("/api/v1/office/8/partners").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/partners").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("KT"))
                .andExpect(jsonPath("$[0].targetUrl").doesNotExist());
        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/partners/1/click").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://kt.example/ovlo"));
        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/partners"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("플랫폼 제휴 관리 API 는 ADMIN 권한이 없으면 403")
    void adminPartnersRequireAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/partners")
                        .header("Authorization", bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_ADMIN))))
                .andExpect(status().isForbidden());
        verify(managePartnerUseCase, never()).findAll();
    }
}
