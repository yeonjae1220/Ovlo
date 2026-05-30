package me.yeonjae.ovlo.shared.security;

import me.yeonjae.ovlo.adapter.out.persistence.repository.MemberJpaRepository;
import me.yeonjae.ovlo.adapter.out.persistence.entity.MemberJpaEntity;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final MemberJpaRepository memberJpaRepository;

    public AdminUserDetailsService(MemberJpaRepository memberJpaRepository) {
        this.memberJpaRepository = memberJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        MemberJpaEntity entity = memberJpaRepository.findByEmail(email)
                .filter(m -> m.getRole() == MemberRole.ADMIN)
                // HIGH-1 fix: 이메일을 에러 메시지에 노출하지 않음
                .orElseThrow(() -> new UsernameNotFoundException("인증에 실패했습니다"));

        return User.builder()
                .username(entity.getEmail())
                .password(entity.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ADMIN")))
                .build();
    }
}
