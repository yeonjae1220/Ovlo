package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.AppleReauthorization;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.auth.exception.AuthException;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.Email;
import me.yeonjae.ovlo.domain.member.model.Major;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.member.model.Password;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 실제 보안 필터 체인을 태워 탈퇴 API 의 Apple 재인증 본문과 오류 매핑을 확인한다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberWithdrawApiTest {

    private static final String APPLE_BODY = """
            {"apple":{"identityToken":"id-token","nonce":"raw-nonce-0123456789",
             "authorizationCode":"auth-code","redirectUri":"https://ovlo.test/login"}}""";

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired SaveMemberPort saveMembers;
    @Autowired JwtTokenProvider tokens;
    @MockitoBean RateLimiterService limiter;
    @MockitoBean WithdrawMemberUseCase withdrawMemberUseCase;

    MockMvc mvc;
    long me;
    long other;
    String token;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("insert into global_universities(name_en,country_code,city,domain) values ('Withdraw University', 'KR', 'Seoul', 'school.edu')");
        long school = jdbc.queryForObject("select max(id) from global_universities", Long.class);
        me = member("withdrawme", school);
        other = member("withdrawother", school);
        token = "Bearer " + tokens.generateAccessToken(new MemberId(me), MemberRole.MEMBER);
    }

    @Test
    @DisplayName("Apple 재인증 본문을 그대로 탈퇴 명령에 싣는다")
    void appleBody_passedToUseCase() throws Exception {
        mvc.perform(delete("/api/v1/members/" + me).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(APPLE_BODY))
                .andExpect(status().isNoContent());

        ArgumentCaptor<WithdrawMemberCommand> command = ArgumentCaptor.forClass(WithdrawMemberCommand.class);
        then(withdrawMemberUseCase).should().withdraw(command.capture());
        assertThat(command.getValue().apple()).isEqualTo(new AppleReauthorization(
                "id-token", "raw-nonce-0123456789", "auth-code", "https://ovlo.test/login"));
    }

    @Test
    @DisplayName("본문 없는 기존 탈퇴 요청은 그대로 동작한다")
    void noBody_stillWorks() throws Exception {
        mvc.perform(delete("/api/v1/members/" + me).header("Authorization", token))
                .andExpect(status().isNoContent());

        then(withdrawMemberUseCase).should().withdraw(new WithdrawMemberCommand(me));
    }

    @Test
    @DisplayName("재인증 본문이 형식에 맞지 않으면 400 — 유스케이스까지 가지 않는다")
    void invalidAppleBody_badRequest() throws Exception {
        mvc.perform(delete("/api/v1/members/" + me).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apple\":{\"identityToken\":\"t\",\"nonce\":\"short\",\"authorizationCode\":\"c\"}}"))
                .andExpect(status().isBadRequest());

        then(withdrawMemberUseCase).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("다른 회원의 탈퇴는 403")
    void otherMember_forbidden() throws Exception {
        mvc.perform(delete("/api/v1/members/" + other).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(APPLE_BODY))
                .andExpect(status().isForbidden());

        then(withdrawMemberUseCase).should(never()).withdraw(any());
    }

    @Test
    @DisplayName("Apple 재인증 오류는 400·403·503 으로, 메시지는 error 필드로 내려간다")
    void appleErrors_mapToStatus() throws Exception {
        expectStatus(AuthException.ErrorType.BAD_REQUEST, 400);
        expectStatus(AuthException.ErrorType.FORBIDDEN, 403);
        expectStatus(AuthException.ErrorType.UNAVAILABLE, 503);
    }

    @Test
    @DisplayName("로그인 방식은 본인 프로필에만 보인다")
    void loginProvider_onlyForSelf() throws Exception {
        mvc.perform(get("/api/v1/members/" + me).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginProvider").value("LOCAL"));
        mvc.perform(get("/api/v1/members/" + other).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginProvider").doesNotExist());
    }

    private void expectStatus(AuthException.ErrorType type, int expected) throws Exception {
        willThrow(new AuthException(type, "apple-error-" + type)).given(withdrawMemberUseCase).withdraw(any());
        mvc.perform(delete("/api/v1/members/" + me).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(APPLE_BODY))
                .andExpect(status().is(expected))
                .andExpect(jsonPath("$.error").value("apple-error-" + type));
    }

    private long member(String nickname, long school) {
        Member member = Member.create(nickname, "Test " + nickname, "Seoul", new Email(nickname + "@withdraw.example"),
                new Password("hash"), new UniversityId(school), new Major("CS", DegreeType.BACHELOR, 1));
        return saveMembers.save(member).getId().value();
    }
}
