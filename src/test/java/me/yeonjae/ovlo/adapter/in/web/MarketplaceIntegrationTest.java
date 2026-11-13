package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.dto.result.VerificationStatusResult;
import me.yeonjae.ovlo.application.dto.command.SendMessageCommand;
import me.yeonjae.ovlo.application.port.in.chat.SendMessageUseCase;
import me.yeonjae.ovlo.application.port.in.marketplace.*;
import me.yeonjae.ovlo.application.port.in.verification.GetMyVerificationStatusQuery;
import me.yeonjae.ovlo.application.port.out.marketplace.*;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.marketplace.model.*;
import me.yeonjae.ovlo.domain.media.model.*;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@Tag("integration") @Testcontainers @SpringBootTest @ActiveProfiles("test")
class MarketplaceIntegrationTest {
    @Container static PostgreSQLContainer<?> pg=new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",pg::getJdbcUrl);r.add("spring.datasource.username",pg::getUsername);r.add("spring.datasource.password",pg::getPassword);
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.flyway.enabled",()->"true");
        r.add("spring.jpa.hibernate.ddl-auto",()->"none");r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.PostgreSQLDialect");
    }
    @Autowired MarketplaceUseCase market;
    @Autowired TranslateListingUseCase translations;
    @Autowired MarketplaceStore store;
    @Autowired TranslationLeasePort leases;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired CleanupMarketplacePhotosUseCase photoCleanup;
    @Autowired UnusedMarketplacePhotoStore unusedPhotos;
    @Autowired SendMessageUseCase messages;
    @Autowired SaveMemberPort members;
    @Autowired JwtTokenProvider tokens;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @MockitoBean RateLimiterService limiter;
    @MockitoBean me.yeonjae.ovlo.application.service.command.SchoolEmailVerificationService verification;
    @MockitoBean ListingTranslationPort translator;
    @MockitoBean StoragePort storage;
    MockMvc mvc; long school,seller,buyer,other; byte[] png;
    @BeforeEach void setup() throws Exception {
        mvc=webAppContextSetup(context).apply(springSecurity()).build();
        school=jdbc.queryForObject("INSERT INTO global_universities(name_en,country_code,city,domain) VALUES('Market University','FR','Paris','school.fr') RETURNING id",Long.class);
        seller=member();buyer=member();other=member();
        when(verification.getByMemberId(anyLong())).thenReturn(new VerificationStatusResult("STUDENT",List.of()));
        when(storage.store(any(),any(),any())).thenAnswer(i->new StoragePath(UUID.randomUUID().toString(),StorageType.LOCAL));
        when(storage.retrieve(any())).thenReturn(new byte[]{1,2,3});
        var out=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20,10,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);png=out.toByteArray();
    }
    long member() {
        String unique=UUID.randomUUID().toString().substring(0,8);
        return members.save(Member.create(unique,"Market member","Paris",new Email(unique+"@example.com"),new Password("hash"),new UniversityId(school),new Major("CS",DegreeType.BACHELOR,1))).getId().value();
    }
    String token(long id) {return "Bearer "+tokens.generateAccessToken(new MemberId(id),MemberRole.MEMBER);}
    ListingDetails details(String title,UUID photo) {
        return new ListingDetails(title,"Pots and plates. Microwave excluded.","en","HOUSEHOLD","GOOD",false,true,true,
                new AskingPrice(new BigDecimal("25000"),"KRW"),new PickupLocation("FR","Paris",school,"Campus gate"),null,null,Set.of(ListingTag.RETURN_HOME),List.of(photo));
    }
    @Test void authenticationOwnershipCurrencyFiltersAndStaleVersion() throws Exception {
        mvc.perform(get("/api/v1/marketplace")).andExpect(status().isUnauthorized());
        var photo=market.upload(seller,png);var l=market.create(seller,details("Kitchen bundle",photo));
        mvc.perform(get("/api/v1/marketplace").header("Authorization",token(buyer)).param("universityId",""+school).param("city","paris").param("tag","RETURN_HOME").param("currency","KRW"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(l.id())).andExpect(jsonPath("$.content[0].details.price.currency").value("KRW"))
                .andExpect(jsonPath("$.content[0].sellerName").exists()).andExpect(jsonPath("$.content[0].email").doesNotExist());
        mvc.perform(patch("/api/v1/marketplace/"+l.id()+"/status").header("Authorization",token(buyer)).contentType("application/json").content("{\"version\":0,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
        market.status(seller,l.id(),0,ListingStatus.RESERVED);
        mvc.perform(patch("/api/v1/marketplace/"+l.id()+"/status").header("Authorization",token(seller)).contentType("application/json").content("{\"version\":0,\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict());
        assertThatThrownBy(()->market.create(buyer,details("Stolen photo",photo))).hasMessageContaining("Photo");
        assertThatThrownBy(()->market.create(seller,details("Duplicate attachment",photo))).hasMessageContaining("another listing");
    }
    @Test void multiplePhotosFreeGiveawayAndUploadValidation() throws Exception {
        var p1=market.upload(seller,png);var p2=market.upload(seller,png);
        String json="""
            {"title":"All free","description":"Everything pictured except the microwave","sourceLanguage":"en","category":"MIXED","condition":"GOOD",
            "free":true,"bundle":true,"partialPickup":false,"price":{"amount":0,"currency":"EUR"},
            "location":{"countryCode":"FR","city":"Paris","universityId":%d,"meetingPoint":"Campus gate"},
            "tags":["RETURN_HOME"],"photoIds":["%s","%s"]}
            """.formatted(school,p2,p1);
        mvc.perform(post("/api/v1/marketplace").header("Authorization",token(seller)).contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.details.photoIds[0]").value(p2.toString())).andExpect(jsonPath("$.details.free").value(true));
        assertThatThrownBy(()->market.upload(seller,"<svg onload='alert(1)'/>".getBytes())).isInstanceOf(IllegalArgumentException.class);
        when(verification.getByMemberId(other)).thenReturn(new VerificationStatusResult("UNVERIFIED",List.of()));
        assertThatThrownBy(()->market.upload(other,png)).hasMessageContaining("Verify");
    }
    @Test void hiddenDeletedAndUnattachedPhotosDoNotLeak() throws Exception {
        var photo=market.upload(seller,png);
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(buyer))).andExpect(status().isNotFound());
        var l=market.create(seller,details("Photo visibility",photo));
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(buyer))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        market.report(buyer,l.id(),"Misleading listing");
        mvc.perform(get("/admin/marketplace").header("Authorization",token(seller))).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/marketplace/"+l.id()).with(user("admin").authorities(()->"ADMIN")).param("version","0").param("hidden","true")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/marketplace/"+l.id()).with(user("admin").authorities(()->"ADMIN")).with(csrf()).param("version","0").param("hidden","true")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/marketplace").with(user("admin").authorities(()->"ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(seller))).andExpect(status().isNotFound());
        assertThatThrownBy(()->market.get(buyer,l.id())).hasMessageContaining("not found");
        market.moderate("admin",l.id(),1,false);market.delete(seller,l.id(),2);
        assertThatThrownBy(()->market.photo(buyer,photo)).hasMessageContaining("not found");
    }
    @Test void concurrentContactCreatesOneRoomAndBlocksStopExistingMessages() throws Exception {
        var l=market.create(seller,details("Chat concurrency",market.upload(seller,png)));
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(6)) {
            List<Future<Long>> calls=new ArrayList<>();
            for(int i=0;i<6;i++) calls.add(pool.submit(()->{start.await();return market.contact(buyer,l.id());}));
            start.countDown();Set<Long> rooms=new HashSet<>();for(var f:calls)rooms.add(f.get(20,TimeUnit.SECONDS));
            assertThat(rooms).hasSize(1);long room=rooms.iterator().next();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM marketplace_chat WHERE listing_id=?",Long.class,l.id())).isEqualTo(1);
            messages.sendMessage(new SendMessageCommand(room,buyer,"Available?"));
            market.block(seller,buyer,true);
            assertThatThrownBy(()->messages.sendMessage(new SendMessageCommand(room,buyer,"Blocked message"))).hasMessageContaining("blocked");
            assertThatThrownBy(()->market.contact(buyer,l.id())).hasMessageContaining("not found");
            market.block(seller,buyer,false);assertThat(market.contact(buyer,l.id())).isEqualTo(room);
            mvc.perform(get("/api/v1/chat/rooms/"+room).header("Authorization",token(other))).andExpect(status().isNotFound());
        }
    }
    @Test void translationsAreCachedByVersionAndNeverReplacePriceOrOriginal() {
        var l=market.create(seller,details("Translation",market.upload(seller,png)));
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenReturn(new ListingTranslationPort.Text("주방 묶음","전자레인지는 제외입니다."));
        var t=translations.translate(buyer,l.id(),"ko");assertThat(t.machineTranslated()).isTrue();
        translations.translate(buyer,l.id(),"ko");verify(translator,times(1)).translateListing(any(),eq("ko"),any(),any());
        assertThat(market.get(buyer,l.id()).details().title()).isEqualTo("Translation");
        assertThat(market.get(buyer,l.id()).details().price().currency()).isEqualTo("KRW");
        market.edit(seller,l.id(),0,details("Changed",l.details().photoIds().getFirst()));
        assertThat(translations.translate(buyer,l.id(),"ko").version()).isEqualTo(1);
        verify(translator,times(2)).translateListing(any(),eq("ko"),any(),any());
        when(translator.translateListing(any(),eq("fr"),any(),any())).thenThrow(new IllegalStateException("provider failed"));
        assertThatThrownBy(()->translations.translate(buyer,l.id(),"fr")).hasMessageContaining("unavailable");
    }
    @Test void translationFinishingAfterEditIsRejected() {
        var l=market.create(seller,details("Stale translation",market.upload(seller,png)));
        when(translator.translateListing(any(),any(),any(),any())).thenAnswer(i->{market.edit(seller,l.id(),0,details("Edited during translation",l.details().photoIds().getFirst()));return new ListingTranslationPort.Text("Old","Old content");});
        assertThatThrownBy(()->translations.translate(buyer,l.id(),"de")).hasMessageContaining("changed");
        assertThat(store.translation(l.id(),0,"de")).isEmpty();
    }
    @Test void concurrentStateChangesAcceptOnlyOneVersionAndSuspendedTokensLoseAccess() throws Exception {
        var l=market.create(seller,details("State concurrency",market.upload(seller,png)));
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();try {market.status(seller,l.id(),0,ListingStatus.RESERVED);return true;} catch(me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException e) {assertThat(e.kind()).isEqualTo(me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.CONFLICT);return false;}});
            var second=pool.submit(()->{start.await();try {market.status(seller,l.id(),0,ListingStatus.COMPLETED);return true;} catch(me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException e) {assertThat(e.kind()).isEqualTo(me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.Kind.CONFLICT);return false;}});
            start.countDown();assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(market.get(buyer,l.id()).version()).isEqualTo(1);
        String oldToken=token(seller);jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",seller);
        mvc.perform(get("/api/v1/marketplace/"+l.id()).header("Authorization",oldToken)).andExpect(status().isUnauthorized());
    }
    @Test void fourDecimalCurrencySurvivesDatabaseRoundTrip() {
        var d=details("Currency precision",market.upload(seller,png));
        var precise=new ListingDetails(d.title(),d.description(),d.sourceLanguage(),d.category(),d.condition(),d.free(),d.bundle(),d.partialPickup(),
                new AskingPrice(new BigDecimal("1.2345"),"CLF"),d.location(),d.pickupFrom(),d.pickupUntil(),d.tags(),d.photoIds());
        var saved=market.create(seller,precise);
        assertThat(market.get(buyer,saved.id()).details().price().amount()).isEqualByComparingTo("1.2345");
    }
    @Test void withdrawnSellerDisappearsFromEveryPublicRead() throws Exception {
        var photo=market.upload(seller,png);
        var listing=market.create(seller,details("Withdrawn seller",photo));
        when(translator.translateListing(any(),any(),any(),any())).thenReturn(new ListingTranslationPort.Text("번역","내용"));
        translations.translate(buyer,listing.id(),"ko"); // A cached translation must also become inaccessible.
        mvc.perform(delete("/api/v1/members/"+seller).header("Authorization",token(seller))).andExpect(status().isNoContent());
        assertSellerNotVisible(listing.id(),photo);
    }
    @Test void suspendedSellerDisappearsAndReturnsAfterReactivation() throws Exception {
        var photo=market.upload(seller,png);
        var listing=market.create(seller,details("Suspended seller",photo));
        jdbc.update("UPDATE member SET status='SUSPENDED' WHERE id=?",seller);
        assertSellerNotVisible(listing.id(),photo);
        jdbc.update("UPDATE member SET status='ACTIVE' WHERE id=?",seller);
        mvc.perform(get("/api/v1/marketplace/"+listing.id()).header("Authorization",token(buyer))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(buyer))).andExpect(status().isOk());
    }
    private void assertSellerNotVisible(long listing,UUID photo) throws Exception {
        mvc.perform(get("/api/v1/marketplace").param("universityId",""+school).header("Authorization",token(buyer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/v1/marketplace/"+listing).header("Authorization",token(buyer))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(buyer))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/marketplace/"+listing+"/translations/ko").header("Authorization",token(buyer))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/marketplace/"+listing+"/contact").header("Authorization",token(buyer))).andExpect(status().isNotFound());
    }
    @Test void concurrentTranslationAcrossServiceInstancesCallsProviderOnce() throws Exception {
        var listing=market.create(seller,details("Concurrent translation",market.upload(seller,png)));
        var entered=new CountDownLatch(1);var followerDenied=new CountDownLatch(1);
        var followerLeases=mock(TranslationLeasePort.class,org.mockito.AdditionalAnswers.delegatesTo(leases));
        doAnswer(i->{boolean acquired=leases.acquire(i.getArgument(0),i.getArgument(1),i.getArgument(2));if(!acquired) followerDenied.countDown();return acquired;})
                .when(followerLeases).acquire(any(),any(),any());
        var second=new me.yeonjae.ovlo.application.service.command.ListingTranslationService(market,store,translator,followerLeases,transactionManager,120,120);
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenAnswer(i->{
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            entered.countDown();assertThat(followerDenied.await(10,TimeUnit.SECONDS)).isTrue();
            return new ListingTranslationPort.Text("제목","본문");
        });
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->translations.translate(buyer,listing.id(),"ko"));
            assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
            var next=pool.submit(()->second.translate(other,listing.id(),"ko"));
            assertThat(first.get(15,TimeUnit.SECONDS)).isEqualTo(next.get(15,TimeUnit.SECONDS));
        } finally { second.close(); }
        verify(translator,times(1)).translateListing(any(),eq("ko"),any(),any());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM marketplace_translation_lease WHERE listing_id=?",Long.class,listing.id())).isZero();
    }
    @Test void replacedTranslationOwnerCannotPublishOrReleaseNewOwner() {
        var listing=market.create(seller,details("Expired worker",market.upload(seller,png)));
        var key=new TranslationLeasePort.Key(listing.id(),0,"ko");var replacement=UUID.randomUUID();
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenAnswer(i->{
            jdbc.update("UPDATE marketplace_translation_lease SET expires_at=clock_timestamp()-interval '1 second' WHERE listing_id=?",listing.id());
            assertThat(leases.acquire(key,replacement,java.time.Duration.ofSeconds(120))).isTrue();
            return new ListingTranslationPort.Text("늦은 응답","폐기할 내용");
        });
        assertThatThrownBy(()->translations.translate(buyer,listing.id(),"ko")).hasMessageContaining("unavailable");
        assertThat(store.translation(listing.id(),0,"ko")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT owner FROM marketplace_translation_lease WHERE listing_id=?",UUID.class,listing.id())).isEqualTo(replacement);
        leases.release(key,replacement);
    }
    @Test void failedTranslationReleasesClaimForRetry() {
        var listing=market.create(seller,details("Retry provider",market.upload(seller,png)));
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenThrow(new IllegalStateException("unavailable"))
                .thenReturn(new ListingTranslationPort.Text("복구","본문"));
        assertThatThrownBy(()->translations.translate(buyer,listing.id(),"ko")).hasMessageContaining("unavailable");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM marketplace_translation_lease WHERE listing_id=?",Long.class,listing.id())).isZero();
        assertThat(translations.translate(buyer,listing.id(),"ko").title()).isEqualTo("복구");
    }
    @Test void cleanupDeletesOnlyOldUnattachedPhotosAndResetsDetachGracePeriod() {
        var abandoned=market.upload(seller,png);var recent=market.upload(seller,png);
        var original=market.upload(seller,png);var replacement=market.upload(seller,png);
        var listing=market.create(seller,details("Replace photo",original));
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id IN (?,?)",abandoned,original);
        market.edit(seller,listing.id(),0,details("New photo",replacement));
        var result=photoCleanup.cleanup();
        assertThat(result.removed()).isEqualTo(1);assertThat(result.failed()).isZero();
        assertThat(store.photo(abandoned)).isEmpty();
        assertThat(store.photo(recent)).isPresent();assertThat(store.photo(original)).isPresent();assertThat(store.photo(replacement)).isPresent();
        assertThat(jdbc.queryForObject("SELECT orphaned_at > CURRENT_TIMESTAMP-interval '1 hour' FROM marketplace_photo WHERE id=?",Boolean.class,original)).isTrue();
        // Linked photos are retained even when the listing is deleted; retention policy is separate.
        market.delete(seller,listing.id(),1);
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id=?",replacement);
        assertThat(photoCleanup.cleanup().removed()).isZero();assertThat(store.photo(replacement)).isPresent();
    }
    @Test void cleanupFailureIsRetriedAndPendingPhotoCannotBeReadOrAttached() throws Exception {
        var photo=market.upload(seller,png);var path=store.photo(photo).orElseThrow().path();
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id=?",photo);
        doThrow(new IllegalStateException("storage unavailable")).doNothing().when(storage).delete(path);
        var result=photoCleanup.cleanup();assertThat(result.failed()).isEqualTo(1);assertThat(store.photo(photo)).isPresent();
        mvc.perform(get("/api/v1/marketplace/photos/"+photo).header("Authorization",token(seller))).andExpect(status().isNotFound());
        assertThatThrownBy(()->market.create(seller,details("Claimed photo",photo))).hasMessageContaining("Photo");
        assertThat(photoCleanup.cleanup().claimed()).isZero(); // backoff, no tight retry loop
        jdbc.update("UPDATE marketplace_photo SET cleanup_retry_at=CURRENT_TIMESTAMP-interval '1 second' WHERE id=?",photo);
        assertThat(photoCleanup.cleanup().removed()).isEqualTo(1);assertThat(store.photo(photo)).isEmpty();
        assertThat(photoCleanup.cleanup().removed()).isZero();verify(storage,times(2)).delete(path);
    }
    @Test void cleanupSkipsPhotoLockedByAnAttachmentInProgress() throws Exception {
        var photo=market.upload(seller,png);
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id=?",photo);
        var locked=new CountDownLatch(1);var finish=new CountDownLatch(1);
        try(var pool=Executors.newSingleThreadExecutor()) {
            var attachment=pool.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(tx->{
                store.validatePhotos(seller,null,List.of(photo));locked.countDown();
                try { assertThat(finish.await(10,TimeUnit.SECONDS)).isTrue(); } catch(InterruptedException e) {throw new RuntimeException(e);}
                return market.create(seller,details("Attach during cleanup",photo));
            }));
            try { assertThat(locked.await(10,TimeUnit.SECONDS)).isTrue();assertThat(photoCleanup.cleanup().claimed()).isZero(); }
            finally { finish.countDown(); }
            assertThat(attachment.get(10,TimeUnit.SECONDS).details().photoIds()).containsExactly(photo);
        }
        assertThat(photoCleanup.cleanup().claimed()).isZero();verify(storage,never()).delete(any());
    }
    @Test void listingPageUsesOneStatementWithOrderedPhotosAndUnchangedPagination() {
        var listings=new ArrayList<me.yeonjae.ovlo.application.dto.result.ListingResult>();
        for(int i=0;i<21;i++) {
            var first=market.upload(seller,png);var second=market.upload(seller,png);var d=details("Page "+i,first);
            var two=new ListingDetails(d.title(),d.description(),d.sourceLanguage(),d.category(),d.condition(),d.free(),d.bundle(),d.partialPickup(),d.price(),d.location(),d.pickupFrom(),d.pickupUntil(),d.tags(),List.of(second,first));
            listings.add(market.create(seller,two));
        }
        var statements=new java.util.concurrent.atomic.AtomicInteger();
        var counted=new org.springframework.jdbc.datasource.DelegatingDataSource(jdbc.getDataSource()) {
            @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
                var connection=super.getConnection();
                return (java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(java.sql.Connection.class.getClassLoader(),new Class<?>[]{java.sql.Connection.class},(proxy,method,args)->{
                    if(method.getName().equals("prepareStatement")) statements.incrementAndGet();
                    try {return method.invoke(connection,args);} catch(java.lang.reflect.InvocationTargetException e) {throw e.getCause();}
                });
            }
        };
        var adapter=new me.yeonjae.ovlo.adapter.out.persistence.MarketplacePersistenceAdapter(new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(counted));
        var filter=new me.yeonjae.ovlo.application.dto.command.MarketplaceSearch("FR","paris",school,null,null,"KRW",ListingTag.RETURN_HOME,false,true,true,false,0);
        var rows=adapter.search(buyer,filter);
        assertThat(statements.get()).isEqualTo(1);assertThat(rows).hasSize(21);
        assertThat(rows.getFirst()).isEqualTo(listings.getLast());
        var page=market.search(buyer,filter);assertThat(page.content()).hasSize(20);assertThat(page.hasNext()).isTrue();
        var next=market.search(buyer,new me.yeonjae.ovlo.application.dto.command.MarketplaceSearch("FR","paris",school,null,null,"KRW",ListingTag.RETURN_HOME,false,true,true,false,1));
        assertThat(next.content()).containsExactly(listings.getFirst());assertThat(next.hasNext()).isFalse();
        market.block(seller,buyer,true);assertThat(adapter.search(buyer,filter)).isEmpty();
    }
    @Test void waitingTranslatorRechecksEachCallersBlockStatus() throws Exception {
        var listing=market.create(seller,details("Blocked follower",market.upload(seller,png)));
        var entered=new CountDownLatch(1);var denied=new CountDownLatch(1);var finish=new CountDownLatch(1);
        var followerLeases=mock(TranslationLeasePort.class,org.mockito.AdditionalAnswers.delegatesTo(leases));
        doAnswer(i->{boolean acquired=leases.acquire(i.getArgument(0),i.getArgument(1),i.getArgument(2));if(!acquired) denied.countDown();return acquired;})
                .when(followerLeases).acquire(any(),any(),any());
        var second=new me.yeonjae.ovlo.application.service.command.ListingTranslationService(market,store,translator,followerLeases,transactionManager,120,120);
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenAnswer(i->{
            entered.countDown();assertThat(finish.await(10,TimeUnit.SECONDS)).isTrue();return new ListingTranslationPort.Text("제목","본문");
        });
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->translations.translate(buyer,listing.id(),"ko"));
            try {
                assertThat(entered.await(10,TimeUnit.SECONDS)).isTrue();
                var next=pool.submit(()->second.translate(other,listing.id(),"ko"));
                assertThat(denied.await(10,TimeUnit.SECONDS)).isTrue();market.block(seller,other,true);
                assertThatThrownBy(()->next.get(5,TimeUnit.SECONDS)).hasCauseInstanceOf(me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException.class);
            } finally {finish.countDown();}
            assertThat(first.get(10,TimeUnit.SECONDS).title()).isEqualTo("제목");
            assertThatThrownBy(()->translations.translate(other,listing.id(),"ko")).hasMessageContaining("not found");
        } finally {second.close();}
        verify(translator,times(1)).translateListing(any(),eq("ko"),any(),any());
    }
    @Test void liveTranslationRenewsLeaseAndWaitTimeoutDoesNotStealIt() {
        var listing=market.create(seller,details("Heartbeat",market.upload(seller,png)));
        var renewed=new CountDownLatch(2);
        var observed=mock(TranslationLeasePort.class,org.mockito.AdditionalAnswers.delegatesTo(leases));
        doAnswer(i->{boolean ok=leases.renew(i.getArgument(0),i.getArgument(1),i.getArgument(2));assertThat(ok).isTrue();renewed.countDown();return ok;})
                .when(observed).renew(any(),any(),any());
        var service=new me.yeonjae.ovlo.application.service.command.ListingTranslationService(market,store,translator,observed,transactionManager,3,1);
        when(translator.translateListing(any(),eq("ko"),any(),any())).thenAnswer(i->{
            assertThat(renewed.await(6,TimeUnit.SECONDS)).isTrue();return new ListingTranslationPort.Text("갱신","본문");
        });
        try {
            assertThat(service.translate(buyer,listing.id(),"ko").title()).isEqualTo("갱신");
            var key=new TranslationLeasePort.Key(listing.id(),0,"de");var owner=UUID.randomUUID();
            assertThat(leases.acquire(key,owner,java.time.Duration.ofSeconds(120))).isTrue();
            assertThatThrownBy(()->service.translate(other,listing.id(),"de")).hasMessageContaining("unavailable");
            assertThat(leases.renew(key,owner,java.time.Duration.ofSeconds(120))).isTrue();
            leases.release(key,owner);verify(translator,never()).translateListing(any(),eq("de"),any(),any());
        } finally {service.close();}
    }
    @Test void actualLocalFilesAreRemovedAndCrashAfterDeleteCanBeRetried(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) {
        var local=new me.yeonjae.ovlo.adapter.out.storage.LocalStorageAdapter(directory.toString());
        var path=local.store(png,MediaType.IMAGE_PNG,"fixture.png");var id=UUID.randomUUID();
        store.addPhoto(new MarketplaceStore.Photo(id,seller,path));
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id=?",id);
        var service=new me.yeonjae.ovlo.application.service.command.MarketplacePhotoCleanupService(unusedPhotos,local,transactionManager,new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),72,100);
        assertThat(service.cleanup().removed()).isEqualTo(1);
        assertThat(java.nio.file.Files.exists(java.nio.file.Path.of(path.path()))).isFalse();assertThat(store.photo(id)).isEmpty();
        var secondPath=local.store(png,MediaType.IMAGE_PNG,"crash.png");var second=UUID.randomUUID();
        store.addPhoto(new MarketplaceStore.Photo(second,seller,secondPath));
        jdbc.update("UPDATE marketplace_photo SET orphaned_at=CURRENT_TIMESTAMP-interval '4 days' WHERE id=?",second);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx->
                unusedPhotos.claim(java.time.Instant.now().minus(java.time.Duration.ofHours(72)),java.time.Instant.now().plusSeconds(3600),100));
        local.delete(secondPath); // Simulated crash before completing the durable pending row.
        jdbc.update("UPDATE marketplace_photo SET cleanup_retry_at=CURRENT_TIMESTAMP-interval '1 second' WHERE id=?",second);
        assertThat(service.cleanup().removed()).isEqualTo(1);assertThat(store.photo(second)).isEmpty();
    }
}
