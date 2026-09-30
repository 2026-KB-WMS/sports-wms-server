package com.kb.wms.inbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 공급처 도메인 특수 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 담는다.
 */
@Getter
public enum SupplierErrorCode implements DomainErrorCode {

    SUPPLIER_NOT_FOUND(ErrorCode.NOT_FOUND, "공급처를 찾을 수 없습니다."),
    DUPLICATE_SUPPLIER_CODE(ErrorCode.CONFLICT, "이미 사용 중인 공급처 코드입니다."),
    SUPPLIER_IN_USE(ErrorCode.CONFLICT, "진행 중인 발주가 있어 비활성화할 수 없습니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    SupplierErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
