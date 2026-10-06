package com.kb.wms.common.config;

import org.springframework.context.annotation.Bean;
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
 * 토큰이 있으면 JwtAuthenticationFilter가 파싱해 인증 주체를 올린다. 인증 도메인 API(/auth/me, /users)만 인증·역할을 요구하고,
 * 나머지 요청은 아직 모두 허용한다.
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
                        .anyRequest().permitAll());

        return http.build();
    }
}
