package me.yeonjae.ovlo.adapter.out.office;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import me.yeonjae.ovlo.shared.metrics.OfficeMetrics;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.application.service.office.NoticeTranslationGate;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
class NoticeTranslationReviewProbeTest {
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"max_tokens","pause_turn","stop_sequence"})
 void truncatedNoticeMustNotPassAdapterAndGate(String stopReason) throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{exchange.getRequestBody().readAllBytes();
   byte[] response="""
    {"id":"msg_test","type":"message","role":"assistant","model":"claude-opus-5-5",
    "content":[{"type":"text","text":"TITLE: Dorm BODY: Fee: 1.5 EUR."}],
    "stop_reason":"%s","stop_sequence":null,"usage":{"input_tokens":2000,"output_tokens":2048}}
    """.formatted(stopReason).getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);
   exchange.getResponseBody().write(response);exchange.close();});
  server.start();
  try {
   var adapter=new AnthropicLlmAdapter(new OfficeMetrics(new SimpleMeterRegistry()),"sk-test","http://127.0.0.1:"+server.getAddress().getPort(),"claude-opus-5-5","medium",2048,10,false);
   var request=new TranslationPort.Request(NoticeLang.KO,NoticeLang.EN,"기숙사","비용은 1.5 EUR. 환불은 불가능합니다. 여권 원본을 반드시 지참하세요.");
   boolean rejected=false;
   try { rejected=!new NoticeTranslationGate().check(request,adapter.translate(request)).passed(); }
   catch(IllegalStateException ex) {rejected=true;}
   assertThat(rejected).as("Truncated translation omitting refund/passport conditions must be rejected").isTrue();
  } finally {server.stop(0);}
 }
 @Test void changedTimeMustBeRejectedEvenWhenOriginalDigitsArePreserved() {
  var request=new TranslationPort.Request(NoticeLang.KO,NoticeLang.EN,"안내","마감은 18시입니다.");
  assertThat(new NoticeTranslationGate().check(request,new TranslationPort.Result("Notice","Deadline is 18:59.","test")).passed()).isFalse();
 }
}
