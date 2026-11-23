package me.yeonjae.ovlo.adapter.out.video;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.yeonjae.ovlo.application.port.out.university.VideoMetadataPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * 유튜브 oEmbed 로 영상의 제목·채널명과 생존 여부를 묻는다. API 키·할당량이 없는 공개 엔드포인트다.
 *
 * <p>상태 코드 해석: 200 은 있음, 400·403·404 는 사라짐(없는 영상은 400 — 2026-11-24 실측,
 * 비공개는 403 으로 알려져 있다), 401 은 '임베드 금지'로 알려져 있어 영상이 살아 있을 수 있으므로
 * 판정하지 않는다. 429 는 제한, 그 밖의 오류는 판정하지 않는다. 오류를 '있음'으로 읽지 않는다.</p>
 */
@Component
public class YoutubeOEmbedAdapter implements VideoMetadataPort {

    /** 기본 'Java/…' 대신 누가 왜 부르는지 알 수 있는 이름을 보낸다. */
    static final String USER_AGENT = "OvloLinkCheck/1.0 (+https://ovlo.mungji.com)";

    private final RestClient restClient;
    private final String endpoint;

    public YoutubeOEmbedAdapter(@Value("${ovlo.video-links.oembed-url:https://www.youtube.com/oembed}") String endpoint) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "application/json")
                .build();
        this.endpoint = endpoint;
    }

    @Override
    public VideoMetadata lookup(String youtubeUrl) {
        try {
            OEmbedResponse body = restClient.get()
                    .uri(endpoint + "?url={url}&format=json", youtubeUrl)
                    .retrieve()
                    .body(OEmbedResponse.class);
            if (body == null) {
                return VideoMetadata.of(Status.UNKNOWN);
            }
            return new VideoMetadata(Status.FOUND, body.title(), body.authorName());
        } catch (RestClientResponseException e) {
            return VideoMetadata.of(switch (e.getStatusCode().value()) {
                case 400, 403, 404 -> Status.GONE;
                case 429 -> Status.THROTTLED;
                default -> Status.UNKNOWN;
            });
        } catch (ResourceAccessException e) {
            return VideoMetadata.of(Status.UNKNOWN);
        } catch (RestClientException e) {
            return VideoMetadata.of(Status.UNKNOWN);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OEmbedResponse(String title, @JsonProperty("author_name") String authorName) {}
}
