package me.yeonjae.ovlo.shared.config;

import me.yeonjae.ovlo.adapter.in.web.ws.ChatSubscriptionInterceptor;
import me.yeonjae.ovlo.shared.security.JwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String corsAllowedOrigins;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor,
                           ChatSubscriptionInterceptor chatSubscriptionInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.chatSubscriptionInterceptor = chatSubscriptionInterceptor;
    }

    /**
     * Spring이 @Configuration CGLIB 프록시를 통해 싱글턴을 보장하므로
     * configureMessageBroker()에서 직접 호출해도 동일 빈 인스턴스가 반환된다.
     * @Bean 등록으로 ApplicationContext 종료 시 scheduler.destroy()가 자동 호출된다.
     */
    @Bean
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = corsAllowedOrigins.split("\\s*,\\s*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000}) // 로드밸런서 idle timeout(60s) 방지
                .setTaskScheduler(heartbeatScheduler()); // @Configuration CGLIB이 싱글턴 보장
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor, chatSubscriptionInterceptor);
    }
}
