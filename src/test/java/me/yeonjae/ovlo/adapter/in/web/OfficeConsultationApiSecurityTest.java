package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationQuery;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationTranscriptQuery;
import me.yeonjae.ovlo.application.port.in.office.GetOfficeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageConsultationUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.in.office.OpenConsultationUseCase;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.ConsultationStatus;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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

/** 상담 API 경계: 동의 버전 없는 요청 400, 담당자 받은함은 그 Office 담당자만, 대화 조회는 학생/담당자 경로가 갈린다. */
@SpringBootTest
@ActiveProfiles("test")
class OfficeConsultationApiSecurityTest {

    private static final long OFFICE_ID = 5L;
    private static final long MEMBER_ID = 42L;

    @Autowired private WebApplicationContext wac;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private OpenConsultationUseCase openUseCase;
    @MockitoBean private ManageConsultationUseCase manageUseCase;
    @MockitoBean private GetConsultationQuery getQuery;
    @MockitoBean private GetConsultationTranscriptQuery transcriptQuery;
    @MockitoBean private GetOfficeQuery getOfficeQuery;
    @MockitoBean private ManageOfficeStaffUseCase manageOfficeStaffUseCase;
    @MockitoBean private AdministerOfficeUseCase administerOfficeUseCase;
    @MockitoBean private AcceptOfficeInvitationUseCase acceptOfficeInvitationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).apply(springSecurity()).build();
    }

    private String bearer(OfficeMembership membership) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(new MemberId(MEMBER_ID), MemberRole.MEMBER, membership);
    }

    private static ConsultationResult result(ConsultationStatus status) {
        return new ConsultationResult(700L, OFFICE_ID, status, 900L, "q", null, NoticeLang.VI, true, 300L, null, null, "v1",
                Instant.now(), null, null, null);
    }

    @Test
    @DisplayName("학생: 동의 버전이 있으면 201, 없으면 400(VALIDATION_ERROR), 유스케이스의 CONSENT_REQUIRED 는 400")
    void open() throws Exception {
        given(openUseCase.open(any())).willReturn(result(ConsultationStatus.OPEN));

        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/consultations").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inquiryId\":900,\"consentVersion\":\"v1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatRoomId").value(300));

        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/consultations").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inquiryId\":900}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        given(openUseCase.open(any())).willThrow(new ConsultationException("동의", ConsultationException.ErrorType.CONSENT_REQUIRED));
        mockMvc.perform(post("/api/v1/office/" + OFFICE_ID + "/consultations").header("Authorization", bearer(null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"consentVersion\":\"v9\",\"message\":\"help\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CONSULTATION_CONSENT_REQUIRED"));
    }

    @Test
    @DisplayName("담당자 받은함·맡기: 다른 Office 403, 같은 Office STAFF 200, 일반 회원 403")
    void staffInbox() throws Exception {
        given(getQuery.findForStaff(eq(OFFICE_ID), any(), any())).willReturn(new PageImpl<>(List.of()));
        given(manageUseCase.assign(any())).willReturn(result(ConsultationStatus.IN_PROGRESS));

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/consultations")
                        .header("Authorization", bearer(new OfficeMembership(7L, OfficeRole.OFFICE_STAFF))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/consultations").header("Authorization", bearer(null)))
                .andExpect(status().isForbidden());
        verify(getQuery, never()).findForStaff(anyLong(), any(), any());

        String staff = bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_STAFF));
        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/consultations").header("Authorization", staff))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/office-admin/" + OFFICE_ID + "/consultations/700/assign").header("Authorization", staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        verify(manageUseCase).assign(org.mockito.ArgumentMatchers.argThat(c -> c.actorMemberId().equals(MEMBER_ID)));
    }

    @Test
    @DisplayName("대화 조회: 학생 경로는 staff=false, 담당자 경로는 staff=true 로 유스케이스에 전달된다; 비로그인 401")
    void transcriptPaths() throws Exception {
        given(transcriptQuery.transcript(anyLong(), anyLong(), anyLong(), anyBoolean(), any(), anyInt())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/consultations/700/messages").param("lang", "vi")
                        .header("Authorization", bearer(null)))
                .andExpect(status().isOk());
        verify(transcriptQuery).transcript(eq(OFFICE_ID), eq(700L), eq(MEMBER_ID), eq(false), eq(NoticeLang.VI), anyInt());

        mockMvc.perform(get("/api/v1/office-admin/" + OFFICE_ID + "/consultations/700/messages")
                        .header("Authorization", bearer(new OfficeMembership(OFFICE_ID, OfficeRole.OFFICE_ADMIN))))
                .andExpect(status().isOk());
        verify(transcriptQuery).transcript(eq(OFFICE_ID), eq(700L), eq(MEMBER_ID), eq(true), eq(NoticeLang.KO), anyInt());

        mockMvc.perform(get("/api/v1/office/" + OFFICE_ID + "/consultations")).andExpect(status().isUnauthorized());
    }
}
