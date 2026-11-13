package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.chat.*;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.service.office.OfficeStaffRemovedEvent;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.chat.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Tag("integration") @Testcontainers @SpringBootTest @ActiveProfiles("test")
class ChatAccessIntegrationTest {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",pg::getJdbcUrl); r.add("spring.datasource.username",pg::getUsername);
        r.add("spring.datasource.password",pg::getPassword); r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.flyway.enabled",()->"true"); r.add("spring.jpa.hibernate.ddl-auto",()->"none");
        r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.PostgreSQLDialect");
    }
    @Autowired JdbcTemplate db;
    @Autowired SaveMemberPort members;
    @Autowired SaveChatPort saveChat;
    @Autowired LoadChatPort chats;
    @Autowired SendMessageUseCase messages;
    @Autowired JwtTokenProvider tokens;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager tx;
    @Autowired WebApplicationContext context;
    @MockitoBean RateLimiterService limiter;
    MockMvc mvc;
    long school, office, student, staff;
    @BeforeEach void setup() {
        mvc=webAppContextSetup(context).apply(springSecurity()).build();
        school=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Security Test','KR') RETURNING id",Long.class);
        office=db.queryForObject("INSERT INTO office(global_univ_id,name,status) VALUES(?,'Test Office','PILOT') RETURNING id",Long.class,school);
        student=member(); staff=member();
        db.update("INSERT INTO office_staff(office_id,member_id,role,status,invited_by,invited_at) VALUES(?,?,'OFFICE_ADMIN','ACTIVE','test',now())",office,staff);
    }
    long member() {
        String id=UUID.randomUUID().toString().substring(0,8);
        return members.save(Member.create(id,"Test","Seoul",new Email(id+"@example.invalid"),new Password("hash"),
            new UniversityId(school),new Major("CS",DegreeType.BACHELOR,1))).getId().value();
    }
    String token(long id) { return "Bearer "+tokens.generateAccessToken(new MemberId(id),MemberRole.MEMBER); }
    long room(long owner,long other,String status) {
        long id=saveChat.save(ChatRoom.create(ChatRoomType.GROUP,"Synthetic consultation",
            List.of(new MemberId(owner),new MemberId(other)))).getId().value();
        db.update("""
            INSERT INTO office_consultation(office_id,member_id,consent_version,consent_text,shared_question,
                shared_lang,status,chat_room_id,opened_at)
            VALUES(?,?,'v2','consent','question','ko',?,?,now())
            """,office,owner,status,id);
        return id;
    }

    @ParameterizedTest @ValueSource(strings={"OPEN","IN_PROGRESS","RESOLVED","CLOSED"})
    void staffRemovalRevokesAllStatusesAndKeepsHistory(String status) throws Exception {
        long room=room(student,staff,status);
        String oldToken=token(staff);
        messages.sendMessage(new SendMessageCommand(room,student,"Retained history"));
        mvc.perform(get("/api/v1/chat/rooms/"+room+"/messages").header("Authorization",oldToken)).andExpect(status().isOk());
        new TransactionTemplate(tx).executeWithoutResult(t-> {
            db.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",office,staff);
            events.publishEvent(new OfficeStaffRemovedEvent(office,staff));
        });
        assertThat(db.queryForObject("SELECT count(*) FROM chat_room_participant WHERE chat_room_id=?",Long.class,room)).isEqualTo(1);
        assertThat(chats.isMember(new ChatRoomId(room),new MemberId(student))).isTrue();
        assertThat(chats.isMember(new ChatRoomId(room),new MemberId(staff))).isFalse();
        assertThat(chats.findByMemberId(new MemberId(staff))).isEmpty();
        assertThat(chats.findMessages(new ChatRoomId(room),0,10)).hasSize(1);
        mvc.perform(get("/api/v1/chat/rooms/"+room+"/messages").header("Authorization",oldToken)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/chat/rooms/"+room+"/read").header("Authorization",oldToken)).andExpect(status().isNotFound());
        assertThatThrownBy(()->messages.sendMessage(new SendMessageCommand(room,staff,"Denied")))
            .isInstanceOf(me.yeonjae.ovlo.domain.chat.exception.ChatException.class);
    }

    @Test void staleParticipationDoesNotAuthorizeRevokedStaffAndOwnStudentRoomSurvives() {
        long consultation=room(student,staff,"CLOSED"), own=room(staff,student,"OPEN");
        db.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",office,staff);
        // Access is denied even before asynchronous participant cleanup.
        assertThat(chats.isMember(new ChatRoomId(consultation),new MemberId(staff))).isFalse();
        new TransactionTemplate(tx).executeWithoutResult(t->events.publishEvent(new OfficeStaffRemovedEvent(office,staff)));
        assertThat(chats.isMember(new ChatRoomId(own),new MemberId(staff))).isTrue();
        assertThat(chats.isMember(new ChatRoomId(consultation),new MemberId(student))).isTrue();
    }

    @Test void suspendedOfficeAndMemberOverrideExistingParticipationAndTokens() throws Exception {
        long room=room(student,staff,"OPEN");
        String oldToken=token(staff);
        db.update("UPDATE office SET status='SUSPENDED' WHERE id=?",office);
        assertThat(chats.isMember(new ChatRoomId(room),new MemberId(staff))).isFalse();
        db.update("UPDATE member SET status='SUSPENDED' WHERE id=?",staff);
        mvc.perform(get("/api/v1/chat/rooms").header("Authorization",oldToken)).andExpect(status().isUnauthorized());
        assertThat(chats.findByMemberId(new MemberId(staff))).isEmpty();
    }

    @Test void ordinaryChatStillWorksAndOutsiderCannotReadOrMarkRead() throws Exception {
        long outsider=member();
        long room=saveChat.save(ChatRoom.create(ChatRoomType.GROUP,"Regular chat",
            List.of(new MemberId(student),new MemberId(staff)))).getId().value();
        mvc.perform(get("/api/v1/chat/rooms/"+room).header("Authorization",token(student))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/chat/rooms/"+room+"/read").header("Authorization",token(outsider))).andExpect(status().isNotFound());
        messages.sendMessage(new SendMessageCommand(room,staff,"Allowed"));
    }
}
