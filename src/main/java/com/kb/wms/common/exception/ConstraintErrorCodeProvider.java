package com.kb.wms.common.exception;

import java.util.Map;

/**
 * DB 제약 조건 이름과 도메인 오류 코드의 대응표를 제공한다.
 *
 * <p>서비스의 사전 중복 검사를 동시 요청이 통과해 DB 유니크 제약에서 막히면
 * {@link GlobalExceptionHandler}가 이 대응표로 사전 검사와 같은 오류 코드(409)를 응답한다.
 * 각 도메인은 자기 제약 조건만 이 인터페이스의 빈으로 등록한다(common이 도메인 패키지를 알지 않도록).
 * 제약 조건 이름은 Flyway 마이그레이션과 JPA 엔티티의 {@code @UniqueConstraint} 이름을 같게 유지한다.
 */
public interface ConstraintErrorCodeProvider {

    /** 제약 조건 이름(소문자) → 도메인 오류 코드 */
    Map<String, DomainErrorCode> constraintErrorCodes();
}
