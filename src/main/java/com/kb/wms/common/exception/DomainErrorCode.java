package com.kb.wms.common.exception;

/**
 * 도메인별 특수 오류 코드 enum이 구현하는 인터페이스.
 * 공통 API 규칙상 오류 코드 기본 세트(ErrorCode)는 확장하지 않고,
 * 도메인 패키지에 이 인터페이스를 구현한 enum을 두어 error_code 문자열을 세분화한다.
 * (예: product 패키지의 ProductErrorCode.PRODUCT_NOT_FOUND)
 */
public interface DomainErrorCode {

    ErrorCode getErrorCode();

    String getDefaultMessage();

    String name();
}
