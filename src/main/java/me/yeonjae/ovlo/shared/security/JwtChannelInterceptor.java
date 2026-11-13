package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import me.yeonjae.ovlo.adapter.in.web.ws.WsSessionKeys;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final LoadMemberPort members;

    public JwtChannelInterceptor(JwtTokenProvider jwtTokenProvider, LoadMemberPort members) {
        this.members = members;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket 연결에 JWT 토큰이 필요합니다");
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateAccessToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다");
        }

        Long memberId = jwtTokenProvider.extractMemberId(token).value();
        if (!members.isActive(new MemberId(memberId))) {
            throw new IllegalArgumentException("활동 중인 회원만 연결할 수 있습니다");
        }
        accessor.getSessionAttributes().put(WsSessionKeys.MEMBER_ID, memberId);
        return message;
    }
}
