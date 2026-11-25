package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.ModerateReportUseCase;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.domain.chat.exception.ChatException;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import me.yeonjae.ovlo.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** 신고·차단을 실제 PostgreSQL 과 보안 필터를 태워 끝까지 확인한다 (App Store 1.2 / Play UGC 정책). */
@Tag("integration") @Testcontainers @SpringBootTest @ActiveProfiles("test")
class ModerationIntegrationTest {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired JdbcTemplate db;
    @Autowired SaveMemberPort members;
    @Autowired SaveChatPort chats;
    @Autowired SendMessageUseCase messages;
    @Autowired ModerateReportUseCase moderation;
    @Autowired JwtTokenProvider tokens;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired WebApplicationContext context;
    @MockitoBean RateLimiterService limiter;

    MockMvc mvc;
    long school, author, viewer, outsider;

    @BeforeEach void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        school = db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Moderation Test','KR') RETURNING id", Long.class);
        author = member("author");
        viewer = member("viewer");
        outsider = member("outsider");
    }

    long member(String prefix) {
        String name = prefix + UUID.randomUUID().toString().substring(0, 8);
        return members.save(Member.create(name, "Synthetic", "City", new Email(name + "@example.invalid"),
                new Password("hash"), new UniversityId(school), new Major("CS", DegreeType.BACHELOR, 1))).getId().value();
    }

    String token(long id) { return "Bearer " + tokens.generateAccessToken(new MemberId(id), MemberRole.MEMBER); }

    long newPost(long authorId) {
        return db.queryForObject("INSERT INTO post(board_id,author_id,title,content,deleted,hidden_by_withdrawal,version) VALUES(1,?,'Title','Body',false,false,0) RETURNING id",
                Long.class, authorId);
    }

    long comment(long postId, long authorId) {
        return db.queryForObject("INSERT INTO comment(post_id,author_id,content) VALUES(?,?,'Comment') RETURNING id", Long.class, postId, authorId);
    }

    long dm(long a, long b) {
        return chats.save(ChatRoom.create(ChatRoomType.DM, null, List.of(new MemberId(a), new MemberId(b)))).getId().value();
    }

    void block(long actor, long target) throws Exception {
        mvc.perform(put("/api/v1/blocks/" + target).header("Authorization", token(actor))).andExpect(status().isNoContent());
    }

    String reportBody(String type, long id, String reason) {
        return "{\"targetType\":\"" + type + "\",\"targetId\":" + id + ",\"reason\":\"" + reason + "\"}";
    }

    @Test @DisplayName("차단하면 양쪽 모두 상대의 게시글·댓글이 피드와 상세에서 사라지고, 풀면 돌아온다")
    void blockHidesContentBothWays() throws Exception {
        long authorPost = newPost(author);
        long viewerPost = newPost(viewer);
        long authorComment = comment(viewerPost, author);

        block(viewer, author);

        mvc.perform(get("/api/v1/posts").param("size", "100").header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) authorPost))))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) viewerPost)));
        mvc.perform(get("/api/v1/posts/" + authorPost).header("Authorization", token(viewer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/posts").param("authorId", String.valueOf(author)).header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/posts/" + viewerPost + "/comments").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/posts/" + viewerPost).header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.comments").isEmpty())
                .andExpect(jsonPath("$.commentCount").value(0));
        // 차단당한 쪽에서도 차단한 사람의 글은 보이지 않는다
        mvc.perform(get("/api/v1/posts/" + viewerPost).header("Authorization", token(author))).andExpect(status().isNotFound());
        // 제3자에게는 그대로 보인다
        mvc.perform(get("/api/v1/posts/" + viewerPost + "/comments").header("Authorization", token(outsider)))
                .andExpect(jsonPath("$.content[0].id").value(authorComment));

        mvc.perform(delete("/api/v1/blocks/" + author).header("Authorization", token(viewer))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/posts/" + authorPost).header("Authorization", token(viewer))).andExpect(status().isOk());
    }

    @Test @DisplayName("차단하면 팔로우가 양방향으로 끊기고, 다시 팔로우하거나 DM 을 열거나 보낼 수 없다")
    void blockCutsFollowAndDirectMessages() throws Exception {
        db.update("INSERT INTO follow(follower_id,followee_id) VALUES(?,?),(?,?)", viewer, author, author, viewer);
        long room = dm(viewer, author);

        block(viewer, author);

        assertThat(db.queryForObject("SELECT count(*) FROM follow WHERE follower_id IN (?,?) AND followee_id IN (?,?)",
                Long.class, viewer, author, viewer, author)).isZero();
        mvc.perform(post("/api/v1/follows").header("Authorization", token(author)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"followeeId\":" + viewer + "}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/chat/rooms").header("Authorization", token(author)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DM\",\"participantIds\":[" + viewer + "]}"))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> messages.sendMessage(new SendMessageCommand(room, author, "hello")))
                .isInstanceOf(ChatException.class)
                .extracting("errorType").isEqualTo(ChatException.ErrorType.FORBIDDEN);
        mvc.perform(get("/api/v1/blocks/" + author).header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.blockedByMe").value(true));
        mvc.perform(get("/api/v1/blocks").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$.content[0].memberId").value(author));
    }

    @Test @DisplayName("차단당한 회원은 차단한 사람의 글에 댓글·반응을 달 수 없고, 차단한 사람의 프로필도 볼 수 없다")
    void blockedMemberCannotInteract() throws Exception {
        long viewerPost = newPost(viewer);
        block(viewer, author);

        mvc.perform(post("/api/v1/posts/" + viewerPost + "/comments").header("Authorization", token(author))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/posts/" + viewerPost + "/reactions").header("Authorization", token(author))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/members/" + viewer).header("Authorization", token(author))).andExpect(status().isNotFound());
        // 차단한 쪽은 해제하려고 상대 프로필을 볼 수 있다
        mvc.perform(get("/api/v1/members/" + author).header("Authorization", token(viewer))).andExpect(status().isOk());
    }

    @Test @DisplayName("구버전 호환 뷰 marketplace_block 은 V57 에서 지워지고 차단 테이블만 남는다")
    void legacyMarketplaceBlockViewIsDropped() {
        assertThat(db.queryForObject("SELECT to_regclass('marketplace_block') IS NULL", Boolean.class)).isTrue();
        assertThat(db.queryForObject("SELECT to_regclass('member_block') IS NOT NULL", Boolean.class)).isTrue();
    }

    @Test @DisplayName("기각된 뒤에는 같은 대상을 다시 신고할 수 있다")
    void reReportAfterDismissal() throws Exception {
        String body = reportBody("MEMBER", author, "HARASSMENT");
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        long first = db.queryForObject("SELECT id FROM content_report WHERE reporter_id=?", Long.class, viewer);
        moderation.resolve("admin", first, ReportResolution.DISMISS);

        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        assertThat(db.queryForObject("SELECT count(*) FROM content_report WHERE reporter_id=? AND status='OPEN'", Long.class, viewer)).isOne();
    }

    @Test @DisplayName("작성자의 수정 트랜잭션 도중 관리자가 게시글을 내리면, 수정 저장이 낙관적 락으로 실패하고 삭제가 유지된다")
    void concurrentAuthorSaveCannotRestoreRemovedPost() throws Exception {
        long live = newPost(author);
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", live, "SPAM"))).andExpect(status().isNoContent());
        long reportId = db.queryForObject("SELECT id FROM content_report WHERE target_id=? AND target_type='POST'", Long.class, live);
        var load = context.getBean(me.yeonjae.ovlo.application.port.out.post.LoadPostPort.class);
        var save = context.getBean(me.yeonjae.ovlo.application.port.out.post.SavePostPort.class);
        var outer = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        var inner = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(tx -> {
            var authorView = load.findById(new me.yeonjae.ovlo.domain.post.model.PostId(live)).orElseThrow();
            inner.executeWithoutResult(t -> moderation.resolve("admin", reportId, ReportResolution.REMOVE_CONTENT));
            authorView.update("Edited", "Edited body");
            save.save(authorView);
        })).isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);

        assertThat(db.queryForObject("SELECT deleted FROM post WHERE id=?", Boolean.class, live)).isTrue();
    }

    @Test @DisplayName("회원 검색에서 차단 관계인 회원이 빠진다")
    void blockHidesMemberFromSearch() throws Exception {
        block(author, viewer);

        mvc.perform(get("/api/v1/members/search").param("nickname", "author").header("Authorization", token(viewer)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) author))));
        mvc.perform(get("/api/v1/members/search").param("nickname", "author").header("Authorization", token(outsider)))
                .andExpect(jsonPath("$[*].id", hasItem((int) author)));
    }

    @Test @DisplayName("채팅 신고 → 중복 신고는 한 건 → 참여자 아닌 회원은 신고 불가 → 관리자가 내리면 메시지가 사라지고 대기 신고가 함께 닫힌다")
    void chatMessageReportLifecycle() throws Exception {
        long room = dm(viewer, author);
        long messageId = messages.sendMessage(new SendMessageCommand(room, author, "abusive")).messageId();
        long other = member("other");
        long groupRoom = chats.save(ChatRoom.create(ChatRoomType.GROUP, "g",
                List.of(new MemberId(author), new MemberId(viewer), new MemberId(other)))).getId().value();
        long groupMessage = messages.sendMessage(new SendMessageCommand(groupRoom, author, "abusive too")).messageId();

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                    .content(reportBody("CHAT_MESSAGE", messageId, "HARASSMENT"))).andExpect(status().isNoContent());
        }
        assertThat(db.queryForObject("SELECT count(*) FROM content_report WHERE target_type='CHAT_MESSAGE' AND target_id=?", Long.class, messageId)).isOne();
        mvc.perform(post("/api/v1/reports").header("Authorization", token(outsider)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("CHAT_MESSAGE", messageId, "SPAM"))).andExpect(status().isNotFound());
        // 두 사람이 같은 단체방 메시지를 신고하면 처리 한 번에 둘 다 닫힌다
        for (long reporter : List.of(viewer, other)) {
            mvc.perform(post("/api/v1/reports").header("Authorization", token(reporter)).contentType(MediaType.APPLICATION_JSON)
                    .content(reportBody("CHAT_MESSAGE", groupMessage, "SPAM"))).andExpect(status().isNoContent());
        }
        long groupReport = db.queryForObject("SELECT min(id) FROM content_report WHERE target_id=?", Long.class, groupMessage);

        moderation.resolve("admin", groupReport, ReportResolution.REMOVE_CONTENT);

        assertThat(db.queryForObject("SELECT count(*) FROM content_report WHERE target_id=? AND status='ACTIONED' AND resolved_by='admin'",
                Long.class, groupMessage)).isEqualTo(2);
        mvc.perform(get("/api/v1/chat/rooms/" + groupRoom + "/messages").header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].messageId", not(hasItem((int) groupMessage))));
        assertThatThrownBy(() -> moderation.resolve("admin", groupReport, ReportResolution.DISMISS))
                .isInstanceOf(ModerationException.class)
                .extracting("errorType").isEqualTo(ModerationException.ErrorType.CONFLICT);
        assertThat(moderation.reports(null)).extracting(r -> r.targetId()).contains(messageId).doesNotContain(groupMessage);
    }

    @Test @DisplayName("자기 글 신고는 400, 지워진 글 신고는 404, 잘못된 사유는 400, 기타 사유는 상세가 필요하다")
    void reportValidation() throws Exception {
        long own = newPost(viewer);
        long deleted = newPost(author);
        db.update("UPDATE post SET deleted=true WHERE id=?", deleted);
        long live = newPost(author);

        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", own, "SPAM"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", deleted, "SPAM"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", live, "NOT_A_REASON"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", live, "OTHER"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", live, "SPAM"))).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("게시글 신고를 처리해 작성자를 정지하면 회원 상태가 SUSPENDED 가 된다")
    void suspendAuthorFromPostReport() throws Exception {
        long live = newPost(author);
        mvc.perform(post("/api/v1/reports").header("Authorization", token(viewer)).contentType(MediaType.APPLICATION_JSON)
                .content(reportBody("POST", live, "HATE"))).andExpect(status().isNoContent());
        long reportId = db.queryForObject("SELECT id FROM content_report WHERE target_id=? AND target_type='POST'", Long.class, live);

        moderation.resolve("admin", reportId, ReportResolution.SUSPEND_AUTHOR);

        assertThat(db.queryForObject("SELECT status FROM member WHERE id=?", String.class, author)).isEqualTo("SUSPENDED");
        assertThat(db.queryForObject("SELECT deleted FROM post WHERE id=?", Boolean.class, live)).isFalse();
    }
}
