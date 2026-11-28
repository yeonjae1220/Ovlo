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
    @Autowired me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort credentials;
    Long office,actor;
    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(NOW);when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        Long university=jdbc.queryForObject("INSERT INTO global_universities(name_en) VALUES ('Source tests ' || gen_random_uuid()) RETURNING id",Long.class);
        int seq=SEQ.incrementAndGet();
        actor=members.save(Member.create("source"+seq,"담당자","Seoul",new Email("source"+seq+"@example.com"),new Password("hashed"),
                new UniversityId(university),new Major("IA",DegreeType.BACHELOR,4))).getId().value();
        Office o=Office.create(university,"국제처",NOW);o.invite(actor,OfficeRole.OFFICE_ADMIN,"test",NOW);o.accept(actor,NOW);
        office=offices.save(o).getId().value();
        credentials.save(me.yeonjae.ovlo.domain.verification.model.VerificationCredential.issueManual(actor,university,null,"test","synthetic",NOW));
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=NoticeLang.class,names={"KO","EN"})
    void unrelatedTranslationCommittedDuringAnswerDoesNotDiscardGrounding(NoticeLang lang) throws Exception {
        Notice snapshot=approved("병행 번역",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        snapshot.putTranslation(NoticeLang.EN,"Deadline","Deadline is November 20.",NOW);
        snapshot=notices.save(snapshot);
        Notice before=snapshot;
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(()->{
                Notice concurrent=notices.findById(before.getId()).orElseThrow();
                concurrent.putTranslation(NoticeLang.FR,"Échéance","Le 20 novembre.",NOW.plusSeconds(1));
                notices.save(concurrent);
            }).get(20,TimeUnit.SECONDS);
        }
        Notice after=notices.findById(snapshot.getId()).orElseThrow();
        assertThat(after.getRowVersion()).isGreaterThan(snapshot.getRowVersion());
        assertThat(after.getVersion()).isEqualTo(snapshot.getVersion());
        Inquiry pending=Inquiry.answered(office,actor,lang,"Deadline?","November 20 [1].",List.of(snapshot.getId()),List.of(snapshot.getId()),NOW,30,"test");
        Inquiry result=finalizer.finish(pending,List.of(snapshot),citation(snapshot),List.of());
        assertThat(result.getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);
        assertThat(knowledge.citations(result.getId())).hasSize(1);
    }

    @Test void usedTranslationReviewKeepsAnswerButChangedTextRejectsIt() {
        Notice snapshot=approved("영어 안내",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        snapshot.putTranslation(NoticeLang.EN,"Deadline","Deadline is November 20.",NOW);snapshot=notices.save(snapshot);
        Notice current=notices.findById(snapshot.getId()).orElseThrow();current.reviewTranslation(NoticeLang.EN);notices.save(current);
        Inquiry pending=Inquiry.answered(office,actor,NoticeLang.EN,"Deadline?","November 20 [1].",List.of(snapshot.getId()),List.of(snapshot.getId()),NOW,30,"test");
        assertThat(finalizer.finish(pending,List.of(snapshot),citation(snapshot),List.of()).getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);
        current=notices.findById(snapshot.getId()).orElseThrow();current.putTranslation(NoticeLang.EN,"Deadline","Deadline is November 21.",NOW.plusSeconds(1));notices.save(current);
        pending=Inquiry.answered(office,actor,NoticeLang.EN,"Deadline?","November 20 [1].",List.of(snapshot.getId()),List.of(snapshot.getId()),NOW,30,"test");
        Inquiry rejected=finalizer.finish(pending,List.of(snapshot),citation(snapshot),List.of());
        assertThat(rejected.getOutcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        assertThat(knowledge.citations(rejected.getId())).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"body","source","archive","hold","review","reapprove","expiry","scope","basis"})
    void materialChangesDuringAnswerStillDiscardCitations(String change) {
        Notice base=approved("원천 자료",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        Notice snapshot=approved("검증 대상",NoticeSource.Type.REVIEWED_STAFF_QA,List.of(base.getId()));
        if(change.equals("scope")) {
            var scope=governance.saveScope(office,actor,new NoticeKnowledgePort.Scope(null,office,"국제처","https://university.example/office",true,0));
            Notice n=draft("웹 안내");n.editContent(n.getCategory(),n.getTitle(),n.getBody(),"https://university.example/office/faq",List.of(),NOW);n=notices.save(n);
            governance.configure(office,n.getId(),actor,n.getRowVersion(),source(NoticeSource.Type.UNIVERSITY_WEB,scope.id(),List.of()));
            n=notices.findById(n.getId()).orElseThrow();governance.review(office,n.getId(),actor,n.getRowVersion(),"VERIFIED","검토",true,true,true,true);
            n=notices.findById(n.getId()).orElseThrow();n.approve(actor.toString(),NOW,TODAY);snapshot=notices.save(n);
            governance.saveScope(office,actor,new NoticeKnowledgePort.Scope(scope.id(),office,scope.department(),scope.url(),false,scope.revision()));
        } else {
            Notice changed=notices.findById(snapshot.getId()).orElseThrow();
            switch(change) {
                case "body" -> { changed.editContent(changed.getCategory(),changed.getTitle(),"정정된 내용",null,List.of(),NOW); changed.verifySource(TODAY);changed.approve(actor.toString(),NOW,TODAY); }
                case "source" -> changed.configureSource(source(NoticeSource.Type.OFFICE_MATERIAL,null,List.of()),NOW);
                case "archive" -> changed.archive();
                case "hold" -> changed.holdSource("철회");
                case "review" -> changed.verifySource(TODAY.plusDays(1));
                case "reapprove" -> { changed.requireReapproval(NOW);changed.approve(actor.toString(),NOW.plusSeconds(1),TODAY); }
                case "expiry" -> when(clock.instant()).thenReturn(NOW.plus(Duration.ofDays(100)));
                case "basis" -> { base.archive();notices.save(base); }
            }
            if(!change.equals("expiry") && !change.equals("basis"))notices.save(changed);
        }
        Inquiry rejected=finalizer.finish(answer(snapshot),List.of(snapshot),citation(snapshot),List.of());
        assertThat(rejected.getOutcome()).isEqualTo(InquiryOutcome.NO_GROUNDS);
        assertThat(knowledge.citations(rejected.getId())).isEmpty();
    }

    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="OVLO_LIVE_LLM",matches="1")
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=NoticeLang.class,names={"KO","EN"})
    @Tag("live-llm")
    void realLlmAnswerOverlapsTranslationCommit(NoticeLang lang) throws Exception {
        var llm=new me.yeonjae.ovlo.adapter.out.office.AnthropicLlmAdapter(
                new me.yeonjae.ovlo.shared.metrics.OfficeMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()),
                System.getenv("ANTHROPIC_API_KEY"),System.getenv().getOrDefault("OVLO_LLM_BASE_URL","https://api.bizrouter.ai"),
                "claude-opus-5-5","medium",2048,90,false,"claude-sonnet-5-5");
        Notice snapshot=approved("서류 제출 안내",NoticeSource.Type.OFFICE_MATERIAL,List.of());
        var gate=new NoticeTranslationGate();
        if(lang==NoticeLang.EN) {
            var seedRequest=new TranslationPort.Request(NoticeLang.KO,NoticeLang.EN,snapshot.getTitle(),snapshot.getBody());
            var seed=llm.translate(seedRequest);assertThat(gate.check(seedRequest,seed).passed()).isTrue();
            snapshot.putTranslation(NoticeLang.EN,seed.title(),seed.body(),NOW);snapshot=notices.save(snapshot);
        }
        Notice before=snapshot;
        var text=before.getTranslation(lang);
        var candidate=new AnswerGenerationPort.Candidate(1,before.getId(),text.map(NoticeTranslation::getTitle).orElse(before.getTitle()),
                text.map(NoticeTranslation::getBody).orElse(before.getBody()),null);
        String question=lang==NoticeLang.KO?"서류 제출 기한은 언제인가요?":"When is the document submission deadline?";
        var request=new AnswerGenerationPort.Request(question,lang,List.of(candidate));
        assertThat(llm.screen(question,List.of()).allowed()).isTrue();
        var start=new CyclicBarrier(2);
        var answerStarted=new java.util.concurrent.atomic.AtomicLong();var answerFinished=new java.util.concurrent.atomic.AtomicLong();
        var translationStarted=new java.util.concurrent.atomic.AtomicLong();var translationFinished=new java.util.concurrent.atomic.AtomicLong();
        AnswerGenerationPort.Result generated;
        TranslationPort.Result translated;
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var answerTask=pool.submit(()->{start.await(10,TimeUnit.SECONDS);answerStarted.set(System.nanoTime());
                assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                try{return llm.answer(request);}finally{answerFinished.set(System.nanoTime());}});
            var translationTask=pool.submit(()->{start.await(10,TimeUnit.SECONDS);translationStarted.set(System.nanoTime());
                var tr=new TranslationPort.Request(NoticeLang.KO,NoticeLang.FR,before.getTitle(),before.getBody());
                TranslationPort.Result result;
                try{result=llm.translate(tr);}finally{translationFinished.set(System.nanoTime());}
                assertThat(gate.check(tr,result).passed()).isTrue();
                Notice current=notices.findById(before.getId()).orElseThrow();
                current.putTranslation(NoticeLang.FR,result.title(),result.body(),NOW);notices.save(current);return result;});
            generated=answerTask.get(100,TimeUnit.SECONDS);translated=translationTask.get(100,TimeUnit.SECONDS);
        }
        long overlap=Math.min(answerFinished.get(),translationFinished.get())-Math.max(answerStarted.get(),translationStarted.get());
        assertThat(overlap).as("Actual provider calls overlap, not just sequential requests").isPositive();
        assertThat(generated.refused()).isFalse();
        var validated=new GroundedAnswerValidator().validate(generated.text(),List.of(candidate));
        assertThat(validated.passed()).isTrue();assertThat(llm.verify(request,validated.validated().text()).allowed()).isTrue();
        Notice after=notices.findById(before.getId()).orElseThrow();assertThat(after.getRowVersion()).isGreaterThan(before.getRowVersion());
        var pending=Inquiry.answered(office,actor,lang,question,validated.validated().text(),List.of(before.getId()),List.of(before.getId()),NOW,30,generated.modelId());
        var result=finalizer.finish(pending,List.of(before),citation(before),List.of());
        var evidence=new LinkedHashMap<String,Object>();
        evidence.put("language",lang.code());evidence.put("answerModel",generated.modelId());evidence.put("translationModel",translated.modelId());
        evidence.put("providerOverlapMs",overlap/1_000_000);evidence.put("revisionBefore",before.getRowVersion());evidence.put("revisionAfter",after.getRowVersion());
        evidence.put("sourceVersionUnchanged",before.getVersion()==after.getVersion());evidence.put("outcome",result.getOutcome().name());
        evidence.put("citationsSaved",knowledge.citations(result.getId()).size());evidence.put("answer",result.getAnswerText());
        var path=java.nio.file.Path.of("build/reports/live-answer-concurrency/"+lang.code()+".json");java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.writeString(path,tools.jackson.databind.json.JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(evidence));
        assertThat(result.getOutcome()).isEqualTo(InquiryOutcome.ANSWERED);assertThat(knowledge.citations(result.getId())).hasSize(1);
    }

}
