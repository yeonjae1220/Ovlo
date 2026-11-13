package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.application.service.office.*;
import me.yeonjae.ovlo.application.service.query.InquiryConversationQueryService;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import me.yeonjae.ovlo.domain.office.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NoticeGovernanceIntegrationTest {
    @Container static final PostgreSQLContainer<?> PG=new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",PG::getJdbcUrl);r.add("spring.datasource.username",PG::getUsername);r.add("spring.datasource.password",PG::getPassword);
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.flyway.enabled",()->"true");
        r.add("spring.jpa.hibernate.ddl-auto",()->"none");r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.PostgreSQLDialect");
    }
    static final Instant NOW=Instant.parse("2026-11-13T04:00:00Z");
    static final LocalDate TODAY=LocalDate.of(2026,11,13);
    static final AtomicInteger SEQ=new AtomicInteger();
    @MockitoBean Clock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MemberPersistenceAdapter members;
    @Autowired OfficePersistenceAdapter offices;
    @Autowired OfficeNoticePersistenceAdapter notices;
    @Autowired NoticeGovernanceService governance;
    @Autowired NoticeKnowledgePort knowledge;
    @Autowired NoticeAnswerFinalizer finalizer;
    Long office,actor;
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(NOW);when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Long university=jdbc.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Source tests') RETURNING id",Long.class);
        int seq=SEQ.incrementAndGet();
        actor=members.save(Member.create("source"+seq,"담당자","Seoul",new Email("source"+seq+"@example.com"),new Password("hashed"),
                new UniversityId(university),new Major("IA",DegreeType.BACHELOR,4))).getId().value();
        Office o=Office.create(university,"국제처",NOW);o.invite(actor,OfficeRole.OFFICE_ADMIN,"test",NOW);o.accept(actor,NOW);
        office=offices.save(o).getId().value();
    }
    Notice draft(String title) { return notices.save(Notice.register(office,NoticeCategory.OTHER,title,"현재 제출 기한은 11월 20일입니다.",null,List.of(),"2027-1",TODAY.minusDays(1),TODAY.plusDays(90),actor,NOW)); }
    NoticeSource source(NoticeSource.Type type,Long scope,List<Long> basis) { return new NoticeSource(type,"국제처","PRIVATE-REFERENCE-123","교환학생","전체",TODAY,null,actor,true,type==NoticeSource.Type.UNIVERSITY_WEB,scope,basis); }
    Notice configured(String title,NoticeSource.Type type,List<Long> basis) {
        Notice n=draft(title);governance.configure(office,n.getId(),actor,n.getRowVersion(),source(type,null,basis));return notices.findById(n.getId()).orElseThrow();
    }
    Notice approved(String title,NoticeSource.Type type,List<Long> basis) {
        Notice n=configured(title,type,basis);
        governance.review(office,n.getId(),actor,n.getRowVersion(),"VERIFIED","공식 담당 부서가 적용 대상과 원문을 확인함",true,true,true,true);
        n=notices.findById(n.getId()).orElseThrow();n.approve(actor.toString(),NOW,TODAY);return notices.save(n);
    }
    @Test void legacyRowsArePreservedButNotEligible() {
        Notice n=draft("legacy");n.approve(actor.toString(),NOW,TODAY);n=notices.save(n);
        assertThat(n.getStatus()).isEqualTo(NoticeStatus.APPROVED);
        assertThat(governance.eligible(n,TODAY)).isFalse();
        assertThat(notices.findActiveForStudent(office,null,TODAY)).isEmpty();
    }
    @Test void qaCannotBeVerifiedWithoutPrivacyAndGeneralizationReview() {
        Notice n=configured("Q&A",NoticeSource.Type.REVIEWED_STAFF_QA,List.of());
        assertThatThrownBy(()->governance.review(office,n.getId(),actor,n.getRowVersion(),"VERIFIED","확인",true,true,false,false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(notices.findById(n.getId()).orElseThrow().sourceEligible(TODAY)).isFalse();
    }
    @Test void officialPagesNeedAnEnabledMatchingScope() {
        var scope=governance.saveScope(office,actor,new NoticeKnowledgePort.Scope(null,office,"국제처","https://university.example/office",true,0));
        Notice n=draft("FAQ");n.editContent(n.getCategory(),n.getTitle(),n.getBody(),"https://university.example/office/faq",List.of(),NOW);n=notices.save(n);
        governance.configure(office,n.getId(),actor,n.getRowVersion(),source(NoticeSource.Type.UNIVERSITY_WEB,scope.id(),List.of()));
        n=notices.findById(n.getId()).orElseThrow();governance.review(office,n.getId(),actor,n.getRowVersion(),"VERIFIED","발행 부서 확인",true,true,false,false);
        n=notices.findById(n.getId()).orElseThrow();n.approve(actor.toString(),NOW,TODAY);n=notices.save(n);
        assertThat(governance.eligible(n,TODAY)).isTrue();
        governance.saveScope(office,actor,new NoticeKnowledgePort.Scope(scope.id(),office,scope.department(),scope.url(),false,scope.revision()));
        assertThat(governance.eligible(notices.findById(n.getId()).orElseThrow(),TODAY)).isFalse();
    }
    @Test void failedReviewDoesNotExtendDeadlineAndChangesInvalidateQa() {
        Notice base=approved("기본 공지",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        Notice qa=approved("기숙사 Q&A",NoticeSource.Type.REVIEWED_STAFF_QA,List.of(base.getId()));
        governance.review(office,base.getId(),actor,base.getRowVersion(),"FAILED","원문 서버 오류",false,false,false,false);
        Notice unchanged=notices.findById(base.getId()).orElseThrow();
        assertThat(unchanged.getReview().nextReviewOn()).isEqualTo(TODAY.plusDays(7));
        assertThat(governance.eligible(unchanged,TODAY.plusDays(8))).isFalse();
        governance.review(office,base.getId(),actor,unchanged.getRowVersion(),"CHANGED","기한 정정 발견",false,false,false,false);
        assertThat(governance.eligible(notices.findById(qa.getId()).orElseThrow(),TODAY)).isFalse();
        assertThat(knowledge.history(office,base.getId())).extracting(NoticeKnowledgePort.Audit::action).contains("SOURCE_CHANGED","VERIFIED","FAILED","CHANGED");
    }
    @Test void twoStaleWritesCannotOverwriteEachOther() {
        Notice n=approved("동시 수정",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        Notice a=notices.findById(n.getId()).orElseThrow(),b=notices.findById(n.getId()).orElseThrow();
        a.changeTerm("2027-2",NOW);notices.save(a);b.changeTerm("2027-S",NOW);
        assertThatThrownBy(()->notices.save(b)).isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
    }
    @Test void concurrentReviewHasExactlyOneWinner() throws Exception {
        Notice n=configured("동시 확인",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        CountDownLatch start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> tasks=new ArrayList<>();
            for(int i=0;i<2;i++) tasks.add(executor.submit(()->{start.await();try {
                governance.review(office,n.getId(),actor,n.getRowVersion(),"VERIFIED","공식 확인",true,true,true,true);return true;
            } catch(me.yeonjae.ovlo.domain.office.exception.NoticeException e) { return false; }}));
            start.countDown();int successes=0;for(var task:tasks) if(task.get(20,TimeUnit.SECONDS)) successes++;
            assertThat(successes).isEqualTo(1);
        }
    }
    Inquiry answer(Notice n) { return Inquiry.answered(office,actor,NoticeLang.KO,"제출 기한은?","11월 20일입니다 [1].",List.of(n.getId()),List.of(n.getId()),NOW,30,"test"); }
    List<NoticeCitation> citation(Notice n) { return List.of(new NoticeCitation(1,n.getId(),n.getVersion(),n.getTitle(),null,n.getSource().type(),n.getSource().department(),TODAY,n.getExpiresAt())); }
    @Test void changedSourceDuringGenerationRejectsAnswer() {
        Notice snapshot=approved("최신 안내",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        governance.review(office,snapshot.getId(),actor,snapshot.getRowVersion(),"WITHDRAWN","담당 부서 철회",false,false,false,false);
        Inquiry result=finalizer.finish(answer(snapshot),List.of(snapshot),citation(snapshot),List.of());
        assertThat(result.getOutcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);assertThat(knowledge.citations(result.getId())).isEmpty();
    }
    @Test void storedCitationsAreImmutableAndDoNotContainPrivateReferences() {
        Notice n=approved("당시 제목",NoticeSource.Type.REVIEWED_STAFF_QA,List.of());
        Inquiry result=finalizer.finish(answer(n),List.of(n),citation(n),List.of());
        assertThat(result.getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);
        n.editContent(n.getCategory(),"변경된 제목",n.getBody(),null,List.of(),NOW);notices.save(n);
        assertThat(knowledge.citations(result.getId())).singleElement().satisfies(c -> assertThat(c.title()).isEqualTo("당시 제목"));
        String json=jdbc.queryForObject("SELECT payload FROM office_inquiry_evidence WHERE id=?",String.class,result.getId());
        assertThat(json).doesNotContain("PRIVATE-REFERENCE-123");
    }
    @Autowired org.springframework.web.context.WebApplicationContext wac;
    @Autowired me.yeonjae.ovlo.shared.security.JwtTokenProvider jwt;
    @Test void sourceApiRejectsAnonymousAndStaleAdminClaims() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(wac)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/office-admin/"+office+"/source-scopes"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        String token=jwt.generateAccessToken(new MemberId(actor),MemberRole.MEMBER,new OfficeMembership(office,OfficeRole.OFFICE_ADMIN));
        jdbc.update("UPDATE office_staff SET role='OFFICE_STAFF' WHERE office_id=? AND member_id=?",office,actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/office-admin/"+office+"/source-scopes")
                .header("Authorization","Bearer "+token).contentType("application/json")
                .content("{\"department\":\"국제처\",\"url\":\"https://university.example/office\",\"enabled\":true,\"revision\":0}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        jdbc.update("UPDATE office_staff SET status='REMOVED' WHERE office_id=? AND member_id=?",office,actor);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/office-admin/"+office+"/review-queue")
                .header("Authorization","Bearer "+token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @Autowired me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase commands;
    @Test void approvalUseCaseRejectsUnreviewedMaterial() {
        Notice n=draft("미검토 자료");
        assertThatThrownBy(() -> commands.approve(new me.yeonjae.ovlo.application.dto.command.NoticeActionCommand(office,n.getId(),actor.toString(),n.getRowVersion())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(notices.findById(n.getId()).orElseThrow().getStatus()).isEqualTo(NoticeStatus.DRAFT);
    }

}
