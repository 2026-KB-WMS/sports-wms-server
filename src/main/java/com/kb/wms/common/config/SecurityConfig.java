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
 * 인가를 적용한 도메인(상품, 창고, 재고, 지점, 입고, 지점 발주, 출고)만 인증·역할을 요구하고, 나머지 요청은 아직 모두 허용한다.
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
                        // 입고: 공급처 조회는 본사+창고 관리자(창고 관리자는 활성만), 발주 등록·입고 쓰기는 창고 관리자, 확정은 본사.
                        // 담당 창고 범위와 발주 취소의 작성자·상태별 권한은 서비스가 검사한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers", "/api/v1/suppliers/*")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/suppliers/**").hasRole("HQ_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/purchase-orders/*/confirm").hasRole("HQ_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/purchase-orders/*/cancel")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/purchase-orders").hasRole("WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/purchase-orders/**", "/api/v1/inbounds/**")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/inbounds/**").hasRole("WAREHOUSE_MANAGER")
                        // 지점 발주: 등록은 점주, 전체 목록·승인·반려·배정은 본사, 보류·재개·부분 종결은 창고 관리자, 취소는 본사+점주.
                        // 단건 조회 범위(담당 지점·배정 창고)와 취소의 작성자·상태별 권한은 서비스가 검사한다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("STORE_OWNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("HQ_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/my").hasAnyRole("STORE_OWNER", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*", "/api/v1/orders/*/details")
                        .hasAnyRole("HQ_ADMIN", "STORE_OWNER", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/cancel").hasAnyRole("HQ_ADMIN", "STORE_OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/hold", "/api/v1/orders/*/resume",
                                "/api/v1/orders/*/complete-partial").hasRole("WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/orders/**").hasRole("HQ_ADMIN")
                        // 출고·재고 할당: 쓰기는 창고 관리자, 조회는 본사+창고 관리자. 점주는 불가.
                        // 담당 창고 범위(발주에 배정된 창고)는 서비스가 검사한다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/allocations/**", "/api/v1/outbounds/**")
                        .hasAnyRole("HQ_ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/v1/allocations/**", "/api/v1/outbounds/**")
                        .hasRole("WAREHOUSE_MANAGER")
                        .anyRequest().permitAll());

        return http.build();
    }
}
