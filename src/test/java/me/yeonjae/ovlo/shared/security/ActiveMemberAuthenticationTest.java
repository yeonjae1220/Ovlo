package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.model.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActiveMemberAuthenticationTest {
    JwtTokenProvider tokens=mock(JwtTokenProvider.class);
    LoadMemberPort members=mock(LoadMemberPort.class);
    MemberId id=new MemberId(7L);
    @BeforeEach void setup() {
        when(tokens.validateAccessToken("existing-jwt")).thenReturn(true);
        when(tokens.extractMemberId("existing-jwt")).thenReturn(id);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void existingHttpTokenStopsWorkingAfterAccountDisable() throws Exception {
        when(tokens.extractRole("existing-jwt")).thenReturn(MemberRole.MEMBER);
        when(tokens.extractOfficeMembership("existing-jwt")).thenReturn(Optional.empty());
        when(members.isActive(id)).thenReturn(true);
        var filter=new JwtAuthenticationFilter(tokens,members);
        var request=new MockHttpServletRequest(); request.addHeader("Authorization","Bearer existing-jwt");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull());
        SecurityContextHolder.clearContext();
        when(members.isActive(id)).thenReturn(false);
        response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->{throw new AssertionError("Disabled account reached handler");});
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void websocketConnectChecksCurrentAccountStatus() {
        var interceptor=new JwtChannelInterceptor(tokens,members);
        var h=StompHeaderAccessor.create(StompCommand.CONNECT);
        h.setSessionAttributes(new HashMap<>()); h.setNativeHeader("Authorization","Bearer existing-jwt"); h.setLeaveMutable(true);
        var message=MessageBuilder.createMessage(new byte[0],h.getMessageHeaders());
        assertThatThrownBy(()->interceptor.preSend(message,null)).isInstanceOf(IllegalArgumentException.class);
        when(members.isActive(id)).thenReturn(true);
        assertThat(interceptor.preSend(message,null)).isSameAs(message);
    }
}
