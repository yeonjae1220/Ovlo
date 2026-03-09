package me.yeonjae.ovlo.shared.config;

import me.yeonjae.ovlo.adapter.in.web.ws.ChatSubscriptionInterceptor;
import me.yeonjae.ovlo.shared.security.JwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;
    private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor,
                           ChatSubscriptionInterceptor chatSubscriptionInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.chatSubscriptionInterceptor = chatSubscriptionInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsAllowedOrigins.split(","))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000}); // 로드밸런서 idle timeout(60s) 방지
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor, chatSubscriptionInterceptor);
    }
}
