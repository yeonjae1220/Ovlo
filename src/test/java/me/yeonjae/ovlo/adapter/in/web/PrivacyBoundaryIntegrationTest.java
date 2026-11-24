package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.chat.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.verification.model.*;
import me.yeonjae.ovlo.shared.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Tag("integration") @Testcontainers @SpringBootTest @ActiveProfiles("test")
class PrivacyBoundaryIntegrationTest {
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
    @Autowired JwtTokenProvider tokens;
    @Autowired WebApplicationContext context;
    @MockitoBean RateLimiterService limiter;
    MockMvc mvc;
    long school, owner, viewer, media;
    @BeforeEach void setup() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        school = db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Privacy Test','KR') RETURNING id", Long.class);
        owner = member(); viewer = member();
        media = db.queryForObject("""
            INSERT INTO media_file(uploader_id,media_type,storage_path,storage_type,original_filename,file_size,created_at)
            VALUES(?,'IMAGE_JPEG','synthetic.jpg','LOCAL','synthetic.jpg',10,now()) RETURNING id
            """, Long.class, owner);
    }
    long member() {
        String name = "u" + UUID.randomUUID().toString().substring(0, 8);
        return members.save(Member.create(name, "Synthetic", "City", new Email(name + "@example.invalid"),
                new Password("hash"), new UniversityId(school), new Major("CS", DegreeType.BACHELOR, 1))).getId().value();
    }
    String token(long id) { return "Bearer " + tokens.generateAccessToken(new MemberId(id), MemberRole.MEMBER); }

    @Test void commentIdentityCountsAndStalePostSaveRemainConsistent() throws Exception {
        long postId = db.queryForObject("INSERT INTO post(board_id,author_id,title,content,deleted,hidden_by_withdrawal,version) VALUES(1,?,'Test','Body',false,false,0) RETURNING id", Long.class, owner);
        var commands = context.getBean(me.yeonjae.ovlo.application.service.command.PostCommandService.class);
        var load = context.getBean(me.yeonjae.ovlo.application.port.out.post.LoadPostPort.class);
        var save = context.getBean(me.yeonjae.ovlo.application.port.out.post.SavePostPort.class);
        var query = context.getBean(me.yeonjae.ovlo.application.port.in.post.GetPostQuery.class);
        var first = commands.createComment(new me.yeonjae.ovlo.application.dto.command.CreateCommentCommand(postId, owner, "First"));
        org.assertj.core.api.Assertions.assertThat(first.id()).isPositive();
        var stale = load.findById(new me.yeonjae.ovlo.domain.post.model.PostId(postId)).orElseThrow();
        commands.deleteComment(new me.yeonjae.ovlo.application.dto.command.DeleteCommentCommand(postId, first.id(), owner));
        commands.createComment(new me.yeonjae.ovlo.application.dto.command.CreateCommentCommand(postId, viewer, "Second"));
        save.save(stale);
        org.assertj.core.api.Assertions.assertThat(query.listByBoard(1L, 0, 20).content().stream().filter(p -> p.id()==postId).findFirst().orElseThrow().commentCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT deleted FROM comment WHERE id=?", Boolean.class, first.id())).isTrue();
        mvc.perform(get("/api/v1/posts/" + postId).header("Authorization", token(viewer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.commentCount").value(1));
    }

    @Test void simultaneousDmCreationReturnsOneRoom() throws Exception {
        var commands = context.getBean(me.yeonjae.ovlo.application.port.in.chat.CreateChatRoomUseCase.class);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Long>>();
            for (int i=0;i<6;i++) futures.add(pool.submit(() -> {
                start.await();
                return commands.createChatRoom(new me.yeonjae.ovlo.application.dto.command.CreateChatRoomCommand(owner, ChatRoomType.DM, null, List.of(owner,viewer))).chatRoomId();
            }));
            start.countDown();
            var ids = new java.util.HashSet<Long>();
            for (var f : futures) ids.add(f.get(20, java.util.concurrent.TimeUnit.SECONDS));
            org.assertj.core.api.Assertions.assertThat(ids).hasSize(1);
        }
    }

    @Test void privateUploadAndProfileAttachmentHaveDifferentVisibility() throws Exception {
        mvc.perform(get("/api/v1/media/" + media)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/media/" + media).header("Authorization", token(owner))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/media/" + media).header("Authorization", token(viewer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/media/" + media + "/file").header("Authorization", token(viewer))).andExpect(status().isNotFound());
        db.update("UPDATE member SET profile_image_media_id=? WHERE id=?", Long.toString(media), owner);
        mvc.perform(get("/api/v1/media/" + media).header("Authorization", token(viewer))).andExpect(status().isOk());
        db.update("INSERT INTO marketplace_block(actor_id,target_id) VALUES(?,?)", viewer, owner);
        mvc.perform(get("/api/v1/media/" + media).header("Authorization", token(viewer))).andExpect(status().isNotFound());
        db.update("DELETE FROM marketplace_block WHERE actor_id=?", viewer);
        db.update("UPDATE member SET status='WITHDRAWN' WHERE id=?", owner);
        mvc.perform(get("/api/v1/media/" + media).header("Authorization", token(viewer))).andExpect(status().isNotFound());
    }

    @Test void noticeAttachmentRequiresCurrentSchoolAccessAndExactPublishedNotice(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var now=java.time.Instant.now();
        var today=java.time.LocalDate.now(me.yeonjae.ovlo.application.service.office.OfficeDates.ZONE);
        var office=Office.create(school,"Synthetic Office",now);
        office.invite(owner,OfficeRole.OFFICE_ADMIN,"test",now); office.accept(owner,now);
        office=context.getBean(me.yeonjae.ovlo.application.port.out.office.SaveOfficePort.class).save(office);
        long officeId=office.getId().value();
        var notice=Notice.register(officeId,NoticeCategory.OTHER,"Fixture","Body",null,List.of(media),"test",today.minusDays(1),today.plusDays(1),owner,now);
        notice.approve(Long.toString(owner),now,today);
        me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,now);
        notice=context.getBean(me.yeonjae.ovlo.application.port.out.office.SaveNoticePort.class).save(notice);
        var file=dir.resolve("fixture.txt"); java.nio.file.Files.writeString(file,"synthetic attachment");
        db.update("UPDATE media_file SET storage_path=? WHERE id=?",file.toString(),media);
        var credentials=context.getBean(me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort.class);
        credentials.save(VerificationCredential.issueManual(viewer,school,null,"test","test",now));
        String endpoint="/api/v1/office/"+officeId+"/notices/"+notice.getId()+"/attachments/"+media;
        mvc.perform(get(endpoint).header("Authorization",token(viewer))).andExpect(status().isOk()).andExpect(content().string("synthetic attachment")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/v1/media/"+media+"/file").header("Authorization",token(viewer))).andExpect(status().isNotFound());
        mvc.perform(get(endpoint).header("Authorization",token(member()))).andExpect(status().isForbidden());
        db.update("UPDATE office_notice SET status='ARCHIVED' WHERE id=?",notice.getId());
        mvc.perform(get(endpoint).header("Authorization",token(viewer))).andExpect(status().isNotFound());
    }

    @Test void anotherMembersUploadCannotBecomeMyAvatar() throws Exception {
        mvc.perform(patch("/api/v1/members/" + viewer + "/profile-image").header("Authorization", token(viewer))
                .contentType("application/json").content("{\"mediaId\":\"" + media + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/members/" + owner + "/profile-image").header("Authorization", token(owner))
                .contentType("application/json").content("{\"mediaId\":\"" + media + "\"}"))
                .andExpect(status().isOk());
    }

    @Test void legacyErasureIsExplicitIdempotentAndLeavesActiveMembersAlone() {
        db.update("UPDATE member SET status='WITHDRAWN' WHERE id=?",owner);
        var erasure=context.getBean(me.yeonjae.ovlo.application.port.out.member.LegacyMemberErasurePort.class);
        String activeEmail=db.queryForObject("SELECT email FROM member WHERE id=?",String.class,viewer);
        org.assertj.core.api.Assertions.assertThat(erasure.pendingCount()).isPositive();
        erasure.eraseBatch(50);
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT email FROM member WHERE id=?",String.class,owner)).endsWith("@deleted.invalid");
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT email FROM member WHERE id=?",String.class,viewer)).isEqualTo(activeEmail);
        org.assertj.core.api.Assertions.assertThat(erasure.eraseBatch(50)).isZero();
    }

    @Test void dmCreationCannotProbeTwoOtherMembers() throws Exception {
        long other = member();
        chats.save(ChatRoom.create(ChatRoomType.DM, "Private synthetic room", List.of(new MemberId(owner), new MemberId(other))));
        mvc.perform(post("/api/v1/chat/rooms").header("Authorization", token(viewer)).contentType("application/json")
                .content("{\"type\":\"DM\",\"participantIds\":[" + owner + "," + other + "]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/chat/rooms").header("Authorization", token(owner)).contentType("application/json")
                .content("{\"type\":\"DM\",\"participantIds\":[" + other + "]}"))
                .andExpect(status().isCreated());
    }

    @Test void withdrawalErasesIdentityAndRejectsOldProfileReads() throws Exception {
        String originalEmail = db.queryForObject("SELECT email FROM member WHERE id=?", String.class, owner);
        db.update("INSERT INTO member_contact_info(member_id,contact_type,contact_value) VALUES(?,'EMAIL','private@example.invalid')", owner);
        mvc.perform(delete("/api/v1/members/" + owner).header("Authorization", token(owner))).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT email FROM member WHERE id=?", String.class, owner)).isNotEqualTo(originalEmail);
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT count(*) FROM member_contact_info WHERE member_id=?", Long.class, owner)).isZero();
        mvc.perform(get("/api/v1/members/" + owner).header("Authorization", token(viewer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/members/" + owner).header("Authorization", token(owner))).andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT count(*) FROM media_deletion_job WHERE media_id=?", Long.class, media)).isEqualTo(1);
    }

    @Test void staleMemberSaveCannotRestoreWithdrawnIdentity() {
        var load = context.getBean(me.yeonjae.ovlo.application.port.out.member.LoadMemberPort.class);
        Member stale = load.findById(new MemberId(owner)).orElseThrow();
        Member withdrawn = load.findById(new MemberId(owner)).orElseThrow();
        withdrawn.withdraw();
        members.save(withdrawn);
        stale.updateBio("late old profile");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> members.save(stale))
                .isInstanceOf(me.yeonjae.ovlo.domain.member.exception.MemberException.class);
        org.assertj.core.api.Assertions.assertThat(load.findById(new MemberId(owner)).orElseThrow().getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test void withdrawalRejectsLatePrivateWritesAtTheDatabaseBoundary() {
        context.getBean(me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase.class)
                .withdraw(new me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand(owner));
        List<java.util.function.LongConsumer> writes = List.of(
                id -> db.update("INSERT INTO media_file(uploader_id,media_type,storage_path,storage_type,original_filename,file_size,created_at) VALUES(?,'IMAGE_JPEG','late.jpg','LOCAL','late.jpg',10,now())", id),
                id -> db.update("INSERT INTO verification_credential(member_id,type,university_id,status,verified_at) VALUES(?,'ADMIN_VERIFIED',?,'VERIFIED',now())", id, school),
                id -> db.update("INSERT INTO personal_event(member_id,title,category,all_day,start_date,end_date,time_zone,created_at,updated_at) VALUES(?,'Late private event','OTHER',true,'2026-11-24','2026-11-25','Asia/Seoul',now(),now())", id));
        for (var write : writes) {
            write.accept(viewer); // Positive control: valid writes from an active member succeed.
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> write.accept(owner))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                    .hasMessageContaining("Member no longer accepts private writes");
        }
    }

    @Test void chatMetadataAndUnreadQueriesRemainBoundedAsMessagesGrow() throws Exception {
        var load=context.getBean(me.yeonjae.ovlo.application.port.out.chat.LoadChatPort.class);
        var report=new StringBuilder("rooms,messages,unread_p95_ms\n");
        var allIds=new java.util.ArrayList<Long>();
        for (int i=0;i<500;i++) {
            long room=db.queryForObject("INSERT INTO chat_room(type,name,created_at) VALUES('GROUP','Synthetic',now()) RETURNING id",Long.class);
            allIds.add(room);
            db.update("INSERT INTO chat_room_participant(chat_room_id,member_id) VALUES(?,?),(?,?)",room,owner,room,viewer);
        }
        db.update("INSERT INTO message(chat_room_id,sender_id,content,sent_at,hidden_by_withdrawal) SELECT p.chat_room_id,?,'Synthetic message',now(),false FROM chat_room_participant p CROSS JOIN generate_series(1,100) g WHERE p.member_id=?",viewer,owner);
        for (int count : new int[]{1,50,500}) {
            var since=new java.util.LinkedHashMap<Long,java.time.Instant>();
            allIds.subList(0,count).forEach(id -> since.put(id,java.time.Instant.EPOCH));
            long[] times=new long[20];
            for(int i=0;i<times.length;i++) {
                long start=System.nanoTime();
                var unread=load.countUnreadBatch(new MemberId(owner),since);
                times[i]=System.nanoTime()-start;
                org.assertj.core.api.Assertions.assertThat(unread).hasSize(count);
                org.assertj.core.api.Assertions.assertThat(unread.values()).allMatch(v -> v==100);
            }
            java.util.Arrays.sort(times);
            report.append(count).append(',').append(count*100).append(',').append(times[18]/1_000_000.0).append('\n');
        }
        var room=load.findById(new ChatRoomId(allIds.getFirst())).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(room.getMessages()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(load.findMessages(room.getId(),0,20)).hasSize(20);
        org.assertj.core.api.Assertions.assertThat(load.findByMemberId(new MemberId(owner))).hasSize(500);
        var dir=java.nio.file.Path.of("build/reports/audit");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve("chat-unread.csv"),report.toString());
    }

    @Test void syntheticBackupRestoresErasureAndDeletionQueue() throws Exception {
        var service=context.getBean(me.yeonjae.ovlo.application.port.in.member.WithdrawMemberUseCase.class);
        service.withdraw(new me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand(owner));
        long started=System.nanoTime();
        var dump=pg.execInContainer("pg_dump","-U",pg.getUsername(),"-d",pg.getDatabaseName(),"-Fc","-f","/tmp/audit-restore.dump");
        org.assertj.core.api.Assertions.assertThat(dump.getExitCode()).isZero();
        long bytes=Long.parseLong(pg.execInContainer("stat","-c","%s","/tmp/audit-restore.dump").getStdout().trim());
        org.assertj.core.api.Assertions.assertThat(bytes).isGreaterThan(1000);
        org.assertj.core.api.Assertions.assertThat(pg.execInContainer("createdb","-U",pg.getUsername(),"audit_restore").getExitCode()).isZero();
        var restore=pg.execInContainer("pg_restore","-U",pg.getUsername(),"-d","audit_restore","/tmp/audit-restore.dump");
        org.assertj.core.api.Assertions.assertThat(restore.getExitCode()).isZero();
        var check=pg.execInContainer("psql","-U",pg.getUsername(),"-d","audit_restore","-Atc",
                "SELECT (status='WITHDRAWN' AND email LIKE '%@deleted.invalid' AND personal_data_erased_at IS NOT NULL)::text FROM member WHERE id="+owner);
        org.assertj.core.api.Assertions.assertThat(check.getExitCode()).isZero();
        org.assertj.core.api.Assertions.assertThat(check.getStdout().trim()).isEqualTo("true");
        var job=pg.execInContainer("psql","-U",pg.getUsername(),"-d","audit_restore","-Atc","SELECT count(*) FROM media_deletion_job WHERE media_id="+media);
        org.assertj.core.api.Assertions.assertThat(job.getStdout().trim()).isEqualTo("1");
        var directory=java.nio.file.Path.of("build/reports/audit");
        java.nio.file.Files.createDirectories(directory);
        java.nio.file.Files.writeString(directory.resolve("synthetic-restore.txt"),
                "Synthetic PostgreSQL dump/restore only. No production backup used.\nbytes="+bytes+"\nelapsed_ms="+(System.nanoTime()-started)/1_000_000+"\nerasure_and_pending_media_cleanup_preserved=true\n");
    }

    @Test void fileCleanupRetriesThenDeletesActualLocalFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        var storage=new me.yeonjae.ovlo.adapter.out.storage.LocalStorageAdapter(dir.toString());
        var path=storage.store(new byte[]{1,2,3},me.yeonjae.ovlo.domain.media.model.MediaType.IMAGE_JPEG,"fixture.jpg");
        db.update("UPDATE media_file SET storage_path=? WHERE id=?",path.path(),media);
        db.update("INSERT INTO media_deletion_job(media_id) VALUES(?)",media);
        var jobs=context.getBean(me.yeonjae.ovlo.application.port.out.media.MediaDeletionPort.class);
        var failed=org.mockito.Mockito.mock(me.yeonjae.ovlo.application.port.out.media.StoragePort.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("synthetic transient failure")).when(failed).delete(org.mockito.ArgumentMatchers.any());
        new me.yeonjae.ovlo.application.service.command.MediaDeletionService(jobs,failed).clean();
        org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.exists(java.nio.file.Path.of(path.path()))).isTrue();
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT count(*) FROM media_deletion_job WHERE media_id=?",Long.class,media)).isEqualTo(1);
        db.update("UPDATE media_deletion_job SET next_attempt_at=now() WHERE media_id=?",media);
        new me.yeonjae.ovlo.application.service.command.MediaDeletionService(jobs,storage).clean();
        org.assertj.core.api.Assertions.assertThat(java.nio.file.Files.exists(java.nio.file.Path.of(path.path()))).isFalse();
        org.assertj.core.api.Assertions.assertThat(db.queryForObject("SELECT count(*) FROM media_file WHERE id=?",Long.class,media)).isZero();
    }
}
