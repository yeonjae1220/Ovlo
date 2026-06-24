package me.yeonjae.ovlo.shared.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.yeonjae.ovlo.shared.security.JwtAuthenticationFilter;
import me.yeonjae.ovlo.shared.security.JwtTokenProvider;
import me.yeonjae.ovlo.shared.security.ServiceTokenAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;
    // CRITICAL-1 fix: 동일 인스턴스를 securityFilterChain과 FilterRegistrationBean이 공유
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // WARN fix: 기본값 http://localhost:3000 → 빈 문자열 (운영환경 CORS_ALLOWED_ORIGINS 필수 설정)
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${admin.email}")
    private String adminEmail;

    /** BCrypt hash — k8s Secret ADMIN_PASSWORD_BCRYPT 에서 주입 */
    @Value("${admin.password.bcrypt}")
    private String adminPasswordBcrypt;

    /** 콘솔 집계용 서비스 토큰 — 미설정 시 /api/internal/** 전부 차단(fail-closed) */
    @Value("${console.internal-token:}")
    private String consoleInternalToken;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    // 공개적으로 알려진 테스트 BCrypt 해시 목록 — 운영 환경 사용 차단
    private static final java.util.Set<String> KNOWN_TEST_HASHES = java.util.Set.of(
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi."
    );

    @PostConstruct
    public void validateAdminCredentials() {
        boolean isTest = Arrays.asList(environment.getActiveProfiles()).contains("test");
        if (isTest) {
            log.info("[Admin] test 프로파일 — admin 자격증명 검증 생략");
            return;
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("[Admin] ADMIN_EMAIL 환경변수가 설정되지 않았습니다.");
        }
        if (adminEmail.endsWith(".local") || adminEmail.endsWith(".example.com")) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_EMAIL 기본값이 운영 환경에서 사용되었습니다. ADMIN_EMAIL을 실제 값으로 설정하세요.");
        }
        if (adminPasswordBcrypt == null
                || adminPasswordBcrypt.contains("placeholder")
                || adminPasswordBcrypt.length() < 60
                || (!adminPasswordBcrypt.startsWith("$2a$")
                    && !adminPasswordBcrypt.startsWith("$2b$")
                    && !adminPasswordBcrypt.startsWith("$2y$"))) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_PASSWORD_BCRYPT가 유효한 BCrypt 해시가 아닙니다.");
        }
        if (KNOWN_TEST_HASHES.contains(adminPasswordBcrypt)) {
            throw new IllegalStateException(
                    "[Admin] ADMIN_PASSWORD_BCRYPT가 공개된 테스트 해시입니다. 운영 환경에서 사용할 수 없습니다.");
        }
        log.info("[Admin] admin 계정 설정이 유효합니다: email={}", adminEmail);
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        var admin = User.builder()
                .username(adminEmail)
                .password(adminPasswordBcrypt)
                .authorities("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public AuthenticationManager adminAuthenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(adminUserDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * 콘솔 집계 전용 Security chain (서비스 토큰 기반, 읽기 전용).
     * /api/internal/** 만 담당. ServiceTokenAuthFilter가 X-Internal-Token을
     * 상수시간 비교로 검증 — 통과해야만 컨트롤러 도달. JWT/세션 필터 미적용.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain internalConsoleFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new ServiceTokenAuthFilter(consoleInternalToken),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Admin 전용 Security chain (세션 기반).
     * /admin/** 경로만 담당. formLogin + httpOnly 세션 쿠키로 인증.
     * JWT 필터를 걸지 않아 accessToken XSS 탈취 위협과 완전히 분리.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .authenticationManager(adminAuthenticationManager())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasAuthority("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().changeSessionId()
                        .maximumSessions(1)
                )
                .csrf(Customizer.withDefaults()) // MEDIUM-1 fix: withDefaults()로 의도 명확화
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'"))
                        .frameOptions(f -> f.deny())
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        // CRITICAL-2 fix: setAllowedOrigins로 교체 — 와일드카드 패턴 허용 안 함
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> {
                            if (isProd()) frame.deny();
                            else frame.sameOrigin();
                        })
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'self'"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/members").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/universities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/global-universities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/exchange-universities/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/university-reports/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // HIGH-5 fix: 람다 AuthorizationDecision → 명시적 분기
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                            .access((auth2, ctx) -> new org.springframework.security.authorization.AuthorizationDecision(!isProd()))
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("ADMIN") // WARN fix: 인증 → ADMIN 전용
                        .requestMatchers("/h2-console/**").hasAuthority("ADMIN") // WARN fix: ADMIN 전용으로 격상
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                // CRITICAL-1 fix: 동일 인스턴스 사용
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CRITICAL-1 fix: 동일 인스턴스를 disabled로 등록해 서블릿 자동 등록 차단.
     * /admin/** 요청에 JWT 필터가 끼어들지 않게 하기 위함.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthenticationFilter> reg =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
