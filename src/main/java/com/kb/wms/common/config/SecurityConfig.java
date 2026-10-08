package com.kb.wms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kb.wms.common.security.JwtAuthenticationFilter;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;

/**
 * Security 설정.
 * 토큰이 있으면 JwtAuthenticationFilter가 파싱해 인증 주체를 올린다. 인증 도메인 API(/auth/me, /users)와
 * 인가를 적용한 도메인(상품, 창고, 재고, 지점, 공급처)만 인증·역할을 요구하고, 나머지 요청은 아직 모두 허용한다.
 * 역할 규칙은 docs/api/authorization.md 표와 맞춘다.
 * TODO: 기존 도메인에 인증·인가를 적용하는 마지막 단계(#170)에서 authorizeHttpRequests를 인증 필수로 전환할 것.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtProvider jwtProvider,
                                           RestSecurityExceptionHandler exceptionHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(exceptionHandler)
                        .accessDeniedHandler(exceptionHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("HQ_ADMIN")
                        // 상품: 조회는 인증된 모든 역할, 등록·수정은 본사만
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").authenticated()
                        .requestMatchers("/api/v1/products/**").hasRole("HQ_ADMIN")
                        // 창고: 앞의 규칙이 먼저 적용되므로 구체적인 경로를 위에 둔다. 담당 창고 범위는 컨트롤러가 검사한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/management-types",
                                "/api/v1/warehouses/section-types").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/my").hasRole("WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/warehouses", "/api/v1/warehouses/managers",
                                "/api/v1/warehouses/sections").hasRole("HQ_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/warehouses/*", "/api/v1/warehouses/*/sections",
                                "/api/v1/warehouses/sections/*").hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/warehouses/**").hasRole("HQ_ADMIN")
                        // 재고·로트: 조정은 창고 관리자만, 조회는 본사+창고 관리자(담당 창고 범위는 컨트롤러가 적용). 점주는 불가
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/adjustments").hasRole("WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/inventory/**", "/api/v1/lots/**")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        // 지점: 창고와 같은 순서 규칙. 담당 지점 범위는 서비스가 검사한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/management-types").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/my").hasRole("STORE_OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores", "/api/v1/stores/managers").hasRole("HQ_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/stores/*").hasAnyRole("HQ_ADMIN", "STORE_OWNER")
                        .requestMatchers("/api/v1/stores/**").hasRole("HQ_ADMIN")
                        // 공급처: 조회는 본사+창고 관리자(창고 관리자는 활성만 서비스가 거른다), 등록·수정·비활성화는 본사.
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers", "/api/v1/suppliers/*")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/suppliers/**").hasRole("HQ_ADMIN")
                        .anyRequest().permitAll());

        return http.build();
    }
}
