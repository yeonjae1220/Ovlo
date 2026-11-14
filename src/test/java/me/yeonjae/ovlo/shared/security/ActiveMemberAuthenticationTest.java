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
        var filter=new JwtAuthenticationFilter(tokens,members,mock(me.yeonjae.ovlo.application.port.out.office.LoadOfficePort.class));
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
    @Test void currentDatabaseRoleOverridesOldAdminClaimAndSuspensionRevokesIt() throws Exception {
        var offices=mock(me.yeonjae.ovlo.application.port.out.office.LoadOfficePort.class);
        var office=me.yeonjae.ovlo.domain.office.model.Office.create(1L,"Test",java.time.Instant.now());
        office.assignId(new me.yeonjae.ovlo.domain.office.model.OfficeId(3L));
        office.invite(id.value(),me.yeonjae.ovlo.domain.office.model.OfficeRole.OFFICE_STAFF,"test",java.time.Instant.now());
        office.accept(id.value(),java.time.Instant.now());
        when(offices.findById(3L)).thenReturn(Optional.of(office));
        when(members.isActive(id)).thenReturn(true);
        when(tokens.extractRole("existing-jwt")).thenReturn(MemberRole.MEMBER);
        when(tokens.extractOfficeMembership("existing-jwt")).thenReturn(Optional.of(new me.yeonjae.ovlo.domain.office.model.OfficeMembership(3L,me.yeonjae.ovlo.domain.office.model.OfficeRole.OFFICE_ADMIN)));
        var filter=new JwtAuthenticationFilter(tokens,members,offices);
        var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer existing-jwt");
        filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{
            var auth=SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities()).extracting("authority").contains("OFFICE_STAFF").doesNotContain("OFFICE_ADMIN");
            assertThat(((me.yeonjae.ovlo.domain.office.model.OfficeMembership)auth.getDetails()).isAdmin()).isFalse();
        });
        SecurityContextHolder.clearContext(); office.suspend();
        filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{
            var auth=SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities()).extracting("authority").doesNotContain("OFFICE_STAFF","OFFICE_ADMIN");
            assertThat(auth.getDetails()).isNull();
        });
    }

}
