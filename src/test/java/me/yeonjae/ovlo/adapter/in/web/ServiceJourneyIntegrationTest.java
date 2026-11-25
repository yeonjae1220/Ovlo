package me.yeonjae.ovlo.adapter.in.web;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.Type;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP/security, PostgreSQL, Redis and STOMP. No production accounts or mocked adapters. */
@Tag("integration") @Testcontainers @ActiveProfiles("test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"lab.feedback.endpoint=http://127.0.0.1:1", "ovlo.search.provider=postgres", "CORS_ALLOWED_ORIGINS=http://localhost:3358"})
class ServiceJourneyIntegrationTest {
    @Container static final PostgreSQLContainer<?> pg=new PostgreSQLContainer<>("postgres:16");
    @Container static final GenericContainer<?> redis=new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static final Path MEDIA;
    static { try { MEDIA=Files.createTempDirectory("ovlo-journey-media-"); } catch(Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",pg::getJdbcUrl); r.add("spring.datasource.username",pg::getUsername);
        r.add("spring.datasource.password",pg::getPassword); r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
        r.add("spring.flyway.enabled",()->true); r.add("spring.jpa.hibernate.ddl-auto",()->"none");
        r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.data.redis.host",redis::getHost); r.add("spring.data.redis.port",()->redis.getMappedPort(6379));
        r.add("ovlo.storage.local.path",MEDIA::toString);
    }
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate db;
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    final JsonMapper json=JsonMapper.builder().build();
    final List<Map<String,Object>> timings=Collections.synchronizedList(new ArrayList<>());
    record User(long id,String token,String cookie,String email) {}

    HttpResponse<String> request(String method,String path,String token,String cookie,Object body,int expected) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(30));
        if(token!=null)b.header("Authorization","Bearer "+token);
        if(cookie!=null)b.header("Cookie",cookie);
        b.header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        long start=System.nanoTime();var response=http.send(b.build(),HttpResponse.BodyHandlers.ofString());
        timings.add(Map.of("method",method,"route",path.replaceAll("/\\d+","/:id"),"status",response.statusCode(),"bytes",response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8).length,"ms",(System.nanoTime()-start)/1_000_000.0));
        assertThat(response.statusCode()).as(method+" "+path+" "+(response.statusCode()==expected?"":response.body())).isEqualTo(expected);
        return response;
    }
    JsonNode call(String method,String path,User user,Object body,int expected) throws Exception {
        String response=request(method,path,user==null?null:user.token(),null,body,expected).body();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }
    User register(String nickname,long school) throws Exception {
        String email=nickname+"@example.invalid";
        var result=call("POST","/api/v1/members",null,Map.of("nickname",nickname,"name","Synthetic User","hometown","Test City","email",email,"password","JourneyTest1!","homeUniversityId",school,"majorName","CS","degreeType","BACHELOR","gradeLevel",1),201);
        var login=request("POST","/api/v1/auth/login",null,null,Map.of("email",email,"password","JourneyTest1!"),200);
        var data=json.readTree(login.body());
        assertThat(data.get("memberId").asLong()).isEqualTo(result.get("id").asLong());
        return new User(data.get("memberId").asLong(),data.get("accessToken").asText(),login.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0],email);
    }
    @Test void simultaneousFirstReadMarkersAreIdempotentAndNeverMoveBackwards() throws Exception {
        long school=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Read Test','KR') RETURNING id",Long.class);
        User reader=register("read_marker",school),other=register("read_other",school);
        long room=call("POST","/api/v1/chat/rooms",reader,Map.of("type","DM","participantIds",List.of(other.id())),201).get("chatRoomId").asLong();
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(8)) {
            var futures=new ArrayList<Future<?>>();
            for(int i=0;i<8;i++)futures.add(pool.submit(()->{start.await();call("POST","/api/v1/chat/rooms/"+room+"/read",reader,null,204);return null;}));
            start.countDown();for(var future:futures)future.get(20,TimeUnit.SECONDS);
        }
        assertThat(db.queryForObject("SELECT count(*) FROM chat_room_read_marker WHERE chat_room_id=? AND member_id=?",Long.class,room,reader.id())).isEqualTo(1);
        db.update("UPDATE chat_room_read_marker SET last_read_at=now()+interval '1 day' WHERE chat_room_id=? AND member_id=?",room,reader.id());
        var future=db.queryForObject("SELECT last_read_at FROM chat_room_read_marker WHERE chat_room_id=? AND member_id=?",java.sql.Timestamp.class,room,reader.id());
        call("POST","/api/v1/chat/rooms/"+room+"/read",reader,null,204);
        assertThat(db.queryForObject("SELECT last_read_at FROM chat_room_read_marker WHERE chat_room_id=? AND member_id=?",java.sql.Timestamp.class,room,reader.id())).isEqualTo(future);
    }
    @Test void signupToFirstActivityRefreshChatAndWithdrawal() throws Exception {
        long school=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Journey University','KR') RETURNING id",Long.class);
        User alice=register("journey_alice",school),bob=register("journey_bob",school);
        var refresh=request("POST","/api/v1/auth/refresh",null,alice.cookie(),null,200);
        alice=new User(alice.id(),json.readTree(refresh.body()).get("accessToken").asText(),refresh.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0],alice.email());
        call("GET","/api/v1/members/"+alice.id(),alice,null,200);
        call("GET","/api/v1/global-universities?keyword=Journey",null,null,200);
        long board=call("POST","/api/v1/boards",alice,Map.of("name","Journey board","category","GENERAL","scope","UNIVERSITY","universityId",school),201).get("id").asLong();
        long post=call("POST","/api/v1/posts",alice,Map.of("boardId",board,"title","Journey first post","content","Synthetic first activity"),201).get("id").asLong();
        long comment=call("POST","/api/v1/posts/"+post+"/comments",bob,Map.of("content","Journey reply"),201).get("id").asLong();
        assertThat(comment).isPositive();
        assertThat(call("GET","/api/v1/posts/"+post,alice,null,200).get("commentCount").asInt()).isEqualTo(1);
        call("PUT","/api/v1/calendar/subscriptions/"+school,alice,null,204);
        var event=call("POST","/api/v1/calendar/personal",alice,Map.of("title","Journey private event","category","OTHER","allDay",true,"startDate","2026-11-24","endDate","2026-11-25","timeZone","Asia/Seoul"),201);
        assertThat(call("GET","/api/v1/calendar?from=2026-11-01&to=2026-12-01",alice,null,200).toString()).contains("Journey private event");
        assertThat(call("GET","/api/v1/calendar?from=2026-11-01&to=2026-12-01",bob,null,200).toString()).doesNotContain("Journey private event");
        long room=call("POST","/api/v1/chat/rooms",alice,Map.of("type","DM","participantIds",List.of(bob.id())),201).get("chatRoomId").asLong();
        chatRoundTrip(alice,room,"First connected message");
        chatRoundTrip(alice,room,"Message after reconnect");
        assertThat(call("GET","/api/v1/chat/rooms/"+room+"/messages",bob,null,200).toString()).contains("First connected message","Message after reconnect");
        schoolAndMarketplaceJourneys(alice,bob,school);
        if("1".equals(System.getenv("OVLO_JOURNEY_BROWSER"))) {
            var reportIds=new ArrayList<Long>();
            for(String asOf:List.of(java.time.LocalDate.now().toString(),"2020-01","missing")) {
                long reportSchool=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES(?,'KR') RETURNING id",Long.class,"Evidence fixture "+asOf);
                long rid=db.queryForObject("INSERT INTO university_report(global_univ_id,status,supported_langs) VALUES(?,'PUBLISHED','{en}') RETURNING id",Long.class,reportSchool);
                String content=asOf.equals("missing")?"{}":json.writeValueAsString(Map.of("as_of",asOf,"sources",List.of(Map.of("url","https://example.invalid/official","title","Official fixture"))));
                db.update("INSERT INTO university_report_translation(report_id,lang,title,body,content) VALUES(?,'en','Journey source guide','Synthetic source fixture',CAST(? AS jsonb))",rid,content);
                reportIds.add(rid);
            }
            var process=new ProcessBuilder("node","frontend/scripts/test-service-journey-browser.cjs");
            process.environment().put("API_URL","http://127.0.0.1:"+port);
            process.environment().put("JOURNEY_POST_ID",Long.toString(post));
            process.environment().put("JOURNEY_BOARD_ID",Long.toString(board));
            process.environment().put("JOURNEY_MEMBER_ID",Long.toString(alice.id()));
            process.environment().put("JOURNEY_ROOM_ID",Long.toString(room));
            process.environment().put("JOURNEY_REPORT_IDS",reportIds.stream().map(Object::toString).collect(java.util.stream.Collectors.joining(",")));
            var child=process.inheritIO().start();
            try { assertThat(child.waitFor(8,TimeUnit.MINUTES)).as("Browser finished").isTrue(); assertThat(child.exitValue()).isZero(); }
            finally { child.descendants().forEach(ProcessHandle::destroy); child.destroy(); }
        }
        call("DELETE","/api/v1/members/"+alice.id(),alice,null,204);
        call("GET","/api/v1/members/"+alice.id(),bob,null,404);
        call("GET","/api/v1/members/"+alice.id(),alice,null,401);
        request("POST","/api/v1/auth/refresh",null,alice.cookie(),null,401);
        request("POST","/api/v1/auth/login",null,null,Map.of("email",alice.email(),"password","JourneyTest1!"),401);
        assertThat(db.queryForObject("SELECT count(*) FROM personal_event WHERE member_id=?",Long.class,alice.id())).isZero();
        User replacement=register("journey_alice",school);
        assertThat(replacement.id()).isNotEqualTo(alice.id());
        assertThat(call("GET","/api/v1/chat/rooms",replacement,null,200).size()).isZero();
        Path report=Path.of("build/reports/service-journey/http.json");Files.createDirectories(report.getParent());
        Files.writeString(report,json.writerWithDefaultPrettyPrinter().writeValueAsString(timings));
    }
    @Autowired me.yeonjae.ovlo.application.port.out.verification.SaveVerificationCredentialPort credentials;
    @Autowired me.yeonjae.ovlo.application.port.out.calendar.CalendarMembershipPort memberships;
    @Autowired me.yeonjae.ovlo.application.port.out.office.SaveOfficePort offices;
    @Autowired me.yeonjae.ovlo.application.port.out.office.SaveNoticePort notices;
    void schoolAndMarketplaceJourneys(User student,User staff,long school) throws Exception {
        var now=java.time.Instant.now();var today=java.time.LocalDate.now(me.yeonjae.ovlo.application.service.office.OfficeDates.ZONE);
        for(var user:List.of(student,staff))credentials.save(me.yeonjae.ovlo.domain.verification.model.VerificationCredential.issueManual(user.id(),school,null,"fixture","fixture",now));
        db.update("UPDATE member SET role='UNIV' WHERE id=?",staff.id());memberships.grant(staff.id(),school);
        var event=call("POST","/api/v1/calendar/schools/"+school+"/events",staff,Map.of("title","Journey school event","category","ACADEMIC","allDay",true,"startDate","2026-11-24","endDate","2026-11-25","timeZone","Asia/Seoul"),201);
        call("POST","/api/v1/calendar/school-events/"+event.get("id").asLong()+"/status",staff,Map.of("action","publish","version",event.get("version").asLong()),200);
        assertThat(call("GET","/api/v1/calendar?from=2026-11-01&to=2026-12-01",student,null,200).toString()).contains("Journey school event");
        var office=me.yeonjae.ovlo.domain.office.model.Office.create(school,"Journey Office",now);
        office.invite(staff.id(),me.yeonjae.ovlo.domain.office.model.OfficeRole.OFFICE_ADMIN,"fixture",now);office.accept(staff.id(),now);
        long officeId=offices.save(office).getId().value();
        var notice=me.yeonjae.ovlo.domain.office.model.Notice.register(officeId,me.yeonjae.ovlo.domain.office.model.NoticeCategory.OTHER,"기숙사 입사일","기숙사 입사일은 2026년 11월 24일입니다.",null,List.of(),"fixture",today.minusDays(1),today.plusDays(10),staff.id(),now);
        notice.approve(Long.toString(staff.id()),now,today);me.yeonjae.ovlo.support.NoticeFixtures.verify(notice,now);notices.save(notice);
        var inquiry=call("POST","/api/v1/office/"+officeId+"/inquiries",student,Map.of("question","기숙사 입사일은 언제인가요?","lang","ko"),201);
        assertThat(inquiry.get("outcome").asText()).isEqualTo("ANSWERED");
        assertThat(inquiry.get("citations").size()).isPositive();
        var consultation=call("POST","/api/v1/office/"+officeId+"/consultations",student,Map.of("inquiryId",inquiry.get("inquiryId").asLong(),"consentVersion","v2","lang","ko"),201);
        assertThat(consultation.get("consentVersion").asText()).isEqualTo("v2");
        call("GET","/api/v1/office/"+officeId+"/consultations/"+consultation.get("id").asLong(),student,null,200);
        chatRoundTrip(student,consultation.get("chatRoomId").asLong(),"Synthetic consultation follow-up");
        var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(20,10,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",png);
        var body=new java.io.ByteArrayOutputStream();String boundary="ovloFixtureBoundary";
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"fixture.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());body.write(png.toByteArray());body.write(("\r\n--"+boundary+"--\r\n").getBytes());
        var upload=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/marketplace/photos")).header("Authorization","Bearer "+student.token()).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(upload.statusCode()).as(upload.body()).isEqualTo(201);String photo=json.readTree(upload.body()).get("id").asText();
        call("GET","/api/v1/marketplace/photos/"+photo,staff,null,404);
        var listingBody=new LinkedHashMap<String,Object>();listingBody.putAll(Map.of("title","Journey free bundle","description","Synthetic household items","sourceLanguage","en","category","MIXED","condition","GOOD","free",true,"bundle",true,"partialPickup",false));
        listingBody.put("price",Map.of("amount",0,"currency","KRW"));listingBody.put("location",Map.of("countryCode","KR","city","Seoul","universityId",school,"meetingPoint","Campus gate"));listingBody.put("tags",List.of("RETURN_HOME"));listingBody.put("photoIds",List.of(photo));
        long listing=call("POST","/api/v1/marketplace",student,listingBody,201).get("id").asLong();
        call("GET","/api/v1/marketplace/"+listing,staff,null,200);
        var image=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/marketplace/photos/"+photo)).header("Authorization","Bearer "+staff.token()).build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(image.statusCode()).isEqualTo(200);assertThat(image.body().length).isGreaterThan(100);assertThat(image.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
        long marketRoom=call("POST","/api/v1/marketplace/"+listing+"/contact",staff,null,200).get("roomId").asLong();
        chatRoundTrip(staff,marketRoom,"Is this bundle available?");
        call("PATCH","/api/v1/marketplace/"+listing+"/status",student,Map.of("version",0,"status","RESERVED"),200);
        call("PATCH","/api/v1/marketplace/"+listing+"/status",student,Map.of("version",0,"status","COMPLETED"),409);
    }
    @Test void blockedDmSendIsRejectedToTheSenderOverStomp() throws Exception {
        long school=db.queryForObject("INSERT INTO global_universities(name_en,country_code) VALUES('Block Test','KR') RETURNING id",Long.class);
        User alice=register("stomp_block_alice",school),bob=register("stomp_block_bob",school);
        long room=call("POST","/api/v1/chat/rooms",alice,Map.of("type","DM","participantIds",List.of(bob.id())),201).get("chatRoomId").asLong();
        call("PUT","/api/v1/blocks/"+alice.id(),bob,null,204);
        var client=new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter() {
            @Override protected boolean supportsMimeType(org.springframework.messaging.MessageHeaders headers) { return true; }
        });
        var errors=new LinkedBlockingQueue<String>();
        var headers=new StompHeaders();headers.add("Authorization","Bearer "+alice.token());
        var session=client.connectAsync("ws://127.0.0.1:"+port+"/ws/websocket",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter() {}).get(15,TimeUnit.SECONDS);
        try {
            session.subscribe("/user/queue/errors",new StompFrameHandler() {
                public Type getPayloadType(StompHeaders h) {return String.class;}
                public void handleFrame(StompHeaders h,Object payload) {errors.offer((String)payload);}
            });
            var destination=new StompHeaders();destination.setDestination("/app/chat/"+room);
            destination.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            // The inbound channel is unordered and the error queue has no room to probe, so resend until the
            // subscription is live. Rejected sends store nothing, so repeats are harmless.
            String error=null;
            for(int attempt=0;attempt<15&&error==null;attempt++) {
                session.send(destination,json.writeValueAsString(Map.of("content","Blocked hello","clientMessageId","client-"+attempt)));
                error=errors.poll(1,TimeUnit.SECONDS);
            }
            assertThat(error).isNotNull();
            var body=json.readTree(error);
            assertThat(body.get("roomId").asLong()).isEqualTo(room);
            assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
            assertThat(body.get("clientMessageId").asText()).startsWith("client-");
        } finally { session.disconnect();client.stop(); }
        assertThat(call("GET","/api/v1/chat/rooms/"+room+"/messages",alice,null,200).toString()).doesNotContain("Blocked hello");
    }
    void chatRoundTrip(User user,long room,String message) throws Exception {
        var client=new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter() {
            @Override protected boolean supportsMimeType(org.springframework.messaging.MessageHeaders headers) { return true; }
        });
        var received=new LinkedBlockingQueue<String>();
        var headers=new StompHeaders();headers.add("Authorization","Bearer "+user.token());
        var session=client.connectAsync("ws://127.0.0.1:"+port+"/ws/websocket",new org.springframework.web.socket.WebSocketHttpHeaders(),headers,new StompSessionHandlerAdapter() {}).get(15,TimeUnit.SECONDS);
        try {
            // The inbound channel is unordered; wait for the subscription to reach the broker before SEND.
            session.subscribe("/topic/chat/"+room,new StompFrameHandler() {
                public Type getPayloadType(StompHeaders h) {return String.class;}
                public void handleFrame(StompHeaders h,Object payload) {received.offer((String)payload);}
            });
            var registry=org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10));
            registry.until(()->hasSubscription(room));
            var destination=new StompHeaders();destination.setDestination("/app/chat/"+room);
            destination.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            session.send(destination,json.writeValueAsString(Map.of("content",message)));
            assertThat(received.poll(15,TimeUnit.SECONDS)).contains(message);
        } finally { session.disconnect();client.stop(); }
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).until(()->!hasSubscription(room));
    }
    @Autowired org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler broker;
    boolean hasSubscription(long room) {
        var h=org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(org.springframework.messaging.simp.SimpMessageType.MESSAGE);
        h.setDestination("/topic/chat/"+room);
        return !broker.getSubscriptionRegistry().findSubscriptions(new org.springframework.messaging.support.GenericMessage<>("probe",h.getMessageHeaders())).isEmpty();
    }
    @AfterAll static void cleanMedia() throws Exception {
        try(var paths=Files.walk(MEDIA)) {for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
    }
}
