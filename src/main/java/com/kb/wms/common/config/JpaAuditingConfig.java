package com.kb.wms.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing(등록일시/수정일시 자동 채움) 설정.
 * {@code @SpringBootApplication} 클래스가 아닌 별도 설정 클래스로 분리한다.
 * {@code @EnableJpaAuditing}을 메인 애플리케이션 클래스에 직접 두면
 * {@code @WebMvcTest}처럼 JPA 인프라를 로드하지 않는 슬라이스 테스트에서도
 * jpaMappingContext 빈을 생성하려다 실패("JPA metamodel must not be empty")하기 때문이다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
