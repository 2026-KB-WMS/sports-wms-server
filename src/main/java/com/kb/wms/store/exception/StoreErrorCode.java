package com.kb.wms.store.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 지점 도메인 특수 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 담는다.
 */
@Getter
public enum StoreErrorCode implements DomainErrorCode {

    STORE_NOT_FOUND(ErrorCode.NOT_FOUND, "지점을 찾을 수 없습니다."),
    DUPLICATE_STORE_CODE(ErrorCode.CONFLICT, "이미 사용 중인 지점 코드입니다."),
    STORE_IN_USE(ErrorCode.CONFLICT, "진행 중인 발주가 있어 비활성화할 수 없습니다."),
    MEMBER_NOT_FOUND(ErrorCode.NOT_FOUND, "지점 소속 정보를 찾을 수 없습니다."),
    ALREADY_ASSIGNED(ErrorCode.CONFLICT, "이미 해당 지점에 배정된 사용자입니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    StoreErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
