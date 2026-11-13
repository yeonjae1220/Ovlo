package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.*;
import me.yeonjae.ovlo.application.port.in.calendar.*;
import me.yeonjae.ovlo.application.port.out.calendar.CalendarMembershipPort;
import me.yeonjae.ovlo.application.port.out.member.*;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static com.jayway.jsonpath.JsonPath.read;

@SpringBootTest @ActiveProfiles("test") @Transactional
class CalendarApiSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired SaveMemberPort saveMembers;
    @Autowired LoadMemberPort loadMembers;
    @Autowired CalendarMembershipPort memberships;
    @Autowired CalendarUseCase commands;
    @Autowired UniversityApplicationUseCase applications;
    @Autowired JwtTokenProvider tokens;
    @MockitoBean RateLimiterService limiter;
    MockMvc mvc;
    long school, otherSchool, student, other, manager, admin;
    String managerToken;
    @BeforeEach void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        school = school("Calendar University"); otherSchool = school("Other University");
        student = member("student", MemberRole.MEMBER); other = member("other", MemberRole.MEMBER);
        manager = member("manager", MemberRole.UNIV); admin = member("admin", MemberRole.ADMIN);
        memberships.grant(manager, school);
        managerToken = token(manager, MemberRole.UNIV);
    }
    @Test void anonymousCalendarAndPrivilegedEndpointsAreClosed() throws Exception {
        mvc.perform(get("/api/v1/calendar").param("from","2026-03-01").param("to","2026-04-01")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/calendar/schools/" + school + "/events").contentType(MediaType.APPLICATION_JSON).content(eventJson("School event", ""))).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/calendar")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/calendar").header("Authorization", "Bearer " + managerToken)).andExpect(status().is3xxRedirection());
    }
    @Test void schoolSignupNeverSelfGrantsRoleAndApprovalRequiresAdminAndCsrf() throws Exception {
        String json = """
                {"nickname":"newstaff","name":"School Staff","email":"staff@school.edu","password":"Strong!123",
                 "universityId":%d,"department":"International Office","message":"Official directory: https://school.edu/staff","role":"ADMIN"}
                """.formatted(school);
        String body = mvc.perform(post("/api/v1/members/university-registration").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.major").doesNotExist()).andReturn().getResponse().getContentAsString();
        long id = ((Number) read(body, "$.id")).longValue();
        var app = applications.mine(id).getFirst();
        assertThat(app.status()).isEqualTo("PENDING"); assertThat(memberships.manages(id, school)).isFalse();
        mvc.perform(post("/admin/calendar/applications/" + app.id()).with(user("operator").authorities(() -> "ADMIN"))
                .param("action","approve").param("note","Verified staff directory").param("version","0")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/calendar/applications/" + app.id()).with(user("operator").authorities(() -> "ADMIN")).with(csrf())
                .param("action","approve").param("note","Verified staff directory").param("version","0")).andExpect(status().is3xxRedirection());
        assertThat(loadMembers.findById(new MemberId(id)).orElseThrow().getRole()).isEqualTo(MemberRole.UNIV);
        assertThat(memberships.manages(id, school)).isTrue();
        mvc.perform(get("/admin/calendar").with(user("operator").authorities(() -> "ADMIN")).param("status","APPROVED").param("schoolId",Long.toString(school)))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("School Staff")));
    }
    @Test void managerIsScopedToAssignedSchoolAndRevocationOverridesOldJwt() throws Exception {
        mvc.perform(post("/api/v1/calendar/schools/" + otherSchool + "/events").header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Wrong school", ""))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/calendar/schools/" + school + "/events").header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("School event", "")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.origin").value("UNIVERSITY")).andExpect(jsonPath("$.status").value("DRAFT"));
        memberships.revoke(manager, school);
        mvc.perform(post("/api/v1/calendar/schools/" + school + "/events").header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Revoked", ""))).andExpect(status().isForbidden());
    }
    @Test void demotionAndSuspensionOverrideOldJwt() throws Exception {
        var m = loadMembers.findById(new MemberId(manager)).orElseThrow(); m.updateRole(MemberRole.MEMBER); saveMembers.save(m);
        mvc.perform(get("/api/v1/calendar/schools/" + school + "/events").header("Authorization","Bearer " + managerToken)
                .param("from","2026-03-01").param("to","2026-04-01")).andExpect(status().isForbidden());
        m.updateRole(MemberRole.UNIV); m.suspend(); saveMembers.save(m);
        mvc.perform(get("/api/v1/calendar/options").header("Authorization","Bearer " + managerToken)).andExpect(status().isUnauthorized());
    }
    @Test void draftsStayPrivatePublishedAndCancelledEventsReachSubscribers() throws Exception {
        commands.subscribe(student, school, true);
        var event = commands.createSchool(CalendarActor.member(manager), school, command("School deadline"));
        calendar(student).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        event = commands.changeStatus(CalendarActor.member(manager), event.id(), "publish", event.version());
        calendar(student).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PUBLISHED"));
        calendar(other).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        commands.changeStatus(CalendarActor.member(manager), event.id(), "cancel", event.version());
        calendar(student).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }
    @Test void curatedSourceIsRequiredAndItsOriginCannotBeForgedOrChangedByManager() throws Exception {
        String adminToken = token(admin, MemberRole.ADMIN);
        mvc.perform(post("/api/v1/calendar/schools/" + school + "/events").header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Curated", ""))).andExpect(status().isBadRequest());
        String source = ",\"sourceUrl\":\"https://school.edu/notice\",\"sourceCheckedOn\":\"2026-01-01\",\"origin\":\"UNIVERSITY\"";
        String body = mvc.perform(post("/api/v1/calendar/schools/" + school + "/events").header("Authorization","Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Curated", source)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.origin").value("OVLO_CURATED")).andReturn().getResponse().getContentAsString();
        long id = ((Number) read(body, "$.id")).longValue();
        mvc.perform(put("/api/v1/calendar/school-events/" + id).header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Edited", ",\"version\":0"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/calendar/school-events/" + id).header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Edited", source + ",\"version\":0")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.origin").value("OVLO_CURATED"));
        assertThat(jdbc.queryForObject("select count(*) from calendar_audit where aggregate_type='UNIVERSITY_EVENT' and aggregate_id=?", Integer.class, id)).isEqualTo(2);
    }
    @Test void privateEventsAreNotReadableOrWritableByAnotherMemberOrAdmin() throws Exception {
        var e = commands.createPersonal(student, command("Private appointment"));
        calendar(other).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        calendar(admin).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        for (long viewer : new long[]{other, admin}) {
            mvc.perform(put("/api/v1/calendar/personal/" + e.id()).header("Authorization","Bearer " + token(viewer, viewer == admin ? MemberRole.ADMIN : MemberRole.MEMBER))
                    .contentType(MediaType.APPLICATION_JSON).content(eventJson("Stolen", ",\"version\":0"))).andExpect(status().isNotFound());
            mvc.perform(delete("/api/v1/calendar/personal/" + e.id()).header("Authorization","Bearer " + token(viewer, MemberRole.MEMBER)).param("version","0")).andExpect(status().isNotFound());
        }
        calendar(student).andExpect(status().isOk()).andExpect(jsonPath("$[0].details.title").value("Private appointment"));
    }
    @Test void staleWritesAreRejectedAndRangeIsBounded() throws Exception {
        var e = commands.createPersonal(student, command("Before"));
        mvc.perform(put("/api/v1/calendar/personal/" + e.id()).header("Authorization","Bearer " + token(student, MemberRole.MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("After", ",\"version\":0"))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/calendar/personal/" + e.id()).header("Authorization","Bearer " + token(student, MemberRole.MEMBER))
                .contentType(MediaType.APPLICATION_JSON).content(eventJson("Stale", ",\"version\":0"))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/calendar").header("Authorization","Bearer " + token(student, MemberRole.MEMBER)).param("from","2026-01-01").param("to","2027-01-01"))
                .andExpect(status().isBadRequest());
    }
    @Test void allDayPeriodOverlappingMonthBoundaryIsIncludedWithoutTimezoneShift() throws Exception {
        commands.createPersonal(student, new SaveCalendarEventCommand("Trip", "", "OTHER", true, LocalDate.of(2026,2,28),LocalDate.of(2026,3,2),null,null,"Asia/Seoul","","",null,"",null,null,null));
        mvc.perform(get("/api/v1/calendar").header("Authorization","Bearer " + token(student, MemberRole.MEMBER)).param("from","2026-03-01").param("to","2026-03-02").param("timeZone","America/Los_Angeles"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].details.period.startDate").value("2026-02-28"));
        mvc.perform(get("/api/v1/calendar").header("Authorization","Bearer " + token(student, MemberRole.MEMBER)).param("from","2026-03-02").param("to","2026-03-03"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void cancelledEventCannotBeRepublishedAndAdminCanRenderAndEditPopulatedCalendar() throws Exception {
        var event = commands.createSchool(CalendarActor.member(manager), school, command("Render this event"));
        mvc.perform(get("/admin/calendar").with(user("operator").authorities(() -> "ADMIN"))
                .param("schoolId",Long.toString(school)).param("month","2026-03").param("edit",event.id().toString()))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Render this event")));
        event = commands.changeStatus(CalendarActor.member(manager),event.id(),"publish",event.version());
        event = commands.changeStatus(CalendarActor.member(manager),event.id(),"cancel",event.version());
        mvc.perform(post("/api/v1/calendar/school-events/" + event.id() + "/status").header("Authorization","Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"publish\",\"version\":" + event.version() + "}"))
                .andExpect(status().isConflict());
    }

    @Test void administratorCanCurateThroughSessionFormAndSeeSourceReviewHint() throws Exception {
        mvc.perform(post("/admin/calendar/events").with(user("operator").authorities(() -> "ADMIN")).with(csrf())
                .param("schoolId",Long.toString(school)).param("month","2026-03").param("title","Official visa dates")
                .param("category","ADMINISTRATION").param("allDay","true").param("startDate","2026-03-01").param("endDate","2026-03-15")
                .param("timeZone","Asia/Seoul").param("sourceUrl","https://school.edu/visa").param("sourceCheckedOn","2026-01-01"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/calendar").with(user("operator").authorities(() -> "ADMIN")).param("schoolId",Long.toString(school)).param("month","2026-03"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Official visa dates")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("출처 재확인 필요")));
        assertThat(jdbc.queryForObject("select count(*) from university_event where origin='OVLO_CURATED' and created_by=0",Integer.class)).isEqualTo(1);
    }
    @Test void invalidSchoolSignupRollsBackAndCannotUseExistingEmail() throws Exception {
        long before = jdbc.queryForObject("select count(*) from member", Long.class);
        assertThatThrownBy(() -> applications.register(new UniversityRegistrationCommand("badstaff","Bad Staff","bad@school.edu","Strong!123", -1L,"Office","Test")))
                .isInstanceOf(me.yeonjae.ovlo.domain.calendar.exception.CalendarException.class);
        assertThat(jdbc.queryForObject("select count(*) from member", Long.class)).isEqualTo(before);
    }
    private org.springframework.test.web.servlet.ResultActions calendar(long id) throws Exception {
        return mvc.perform(get("/api/v1/calendar").header("Authorization","Bearer " + token(id, MemberRole.MEMBER)).param("from","2026-03-01").param("to","2026-04-01"));
    }
    private long school(String name) {
        jdbc.update("insert into global_universities(name_en,country_code,city,domain) values (?, 'KR', 'Seoul', 'school.edu')", name);
        return jdbc.queryForObject("select max(id) from global_universities", Long.class);
    }
    private long member(String nick, MemberRole role) {
        var m = Member.create(nick,"Test " + nick,"Seoul",new Email(nick + "@calendar.example"),new Password("hash"), new UniversityId(school), new Major("CS",DegreeType.BACHELOR,1));
        m.updateRole(role); return saveMembers.save(m).getId().value();
    }
    private String token(long id, MemberRole role) { return tokens.generateAccessToken(new MemberId(id), role); }
    private SaveCalendarEventCommand command(String title) {
        return new SaveCalendarEventCommand(title,"","EVENT",true,LocalDate.of(2026,3,1),LocalDate.of(2026,3,16),null,null,"Asia/Seoul","","",null,"",null,null,null);
    }
    private String eventJson(String title, String extras) {
        return "{\"title\":\"" + title + "\",\"category\":\"EVENT\",\"allDay\":true,\"startDate\":\"2026-03-01\",\"endDate\":\"2026-03-16\",\"timeZone\":\"Asia/Seoul\"" + extras + "}";
    }
}
