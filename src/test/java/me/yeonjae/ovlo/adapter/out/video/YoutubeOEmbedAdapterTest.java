package me.yeonjae.ovlo.adapter.out.video;

import com.sun.net.httpserver.HttpServer;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort.Status;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort.VideoMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 HTTP 로 상태 코드 해석을 검증한다(로컬 서버 — 유튜브에는 요청하지 않는다). */
class YoutubeOEmbedAdapterTest {

    private HttpServer server;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastUserAgent = new AtomicReference<>();
    private volatile String contentType = "application/json";
    private volatile int status = 200;
    private volatile String body = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oembed", exchange -> {
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            lastUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("200 이면 제목과 채널명을 돌려주고, 영상 주소는 인코딩된 질의 값으로 보낸다")
    void found() {
        body = "{\"title\":\"와세다 교환학생 후기\",\"author_name\":\"국제교류팀\",\"type\":\"video\"}";

        VideoMetadata result = adapter().lookup("https://www.youtube.com/watch?v=abcdefghijk&t=1");

        assertThat(result).isEqualTo(new VideoMetadata(Status.FOUND, "와세다 교환학생 후기", "국제교류팀"));
        assertThat(lastQuery.get()).contains("format=json").contains("watch%3Fv%3Dabcdefghijk%26t%3D1");        assertThat(lastUserAgent.get()).isEqualTo(YoutubeOEmbedAdapter.USER_AGENT);
    }

    @Test
    @DisplayName("200 이어도 JSON 이 아니면(차단 안내 페이지 등) 있음으로 읽지 않는다")
    void htmlBodyIsNotFound() {
        contentType = "text/html";
        body = "<html><body>Before you continue</body></html>";

        assertThat(adapter().lookup("https://www.youtube.com/watch?v=abcdefghijk").status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("200 이지만 제목이 없는 JSON 은 제목 없이 돌려준다 — 덮어쓸지는 호출자가 정한다")
    void emptyJson() {
        body = "{}";

        assertThat(adapter().lookup("https://www.youtube.com/watch?v=abcdefghijk"))
                .isEqualTo(new VideoMetadata(Status.FOUND, null, null));
    }

    @ParameterizedTest(name = "HTTP {0} → {1}")
    @CsvSource({"400,GONE", "403,GONE", "404,GONE", "401,UNKNOWN", "429,THROTTLED", "500,UNKNOWN", "503,UNKNOWN"})
    @DisplayName("오류 상태 코드는 '있음'으로 읽지 않는다")
    void errorStatuses(int code, Status expected) {
        status = code;

        assertThat(adapter().lookup("https://www.youtube.com/watch?v=abcdefghijk").status()).isEqualTo(expected);
    }

    @Test
    @DisplayName("연결할 수 없으면 판정하지 않는다")
    void unreachable() {
        int port = server.getAddress().getPort();
        server.stop(0);

        assertThat(new YoutubeOEmbedAdapter("http://127.0.0.1:" + port + "/oembed")
                .lookup("https://www.youtube.com/watch?v=abcdefghijk").status()).isEqualTo(Status.UNKNOWN);
    }

    private YoutubeOEmbedAdapter adapter() {
        return new YoutubeOEmbedAdapter("http://127.0.0.1:" + server.getAddress().getPort() + "/oembed");
    }
}
