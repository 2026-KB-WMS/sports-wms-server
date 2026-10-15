package com.kb.wms.inbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 공급처 발주서 전용 오류 코드. 상태 충돌(409)은 공통 코드(CONFLICT)를 쓴다.
 */
@Getter
public enum SupplierOrderErrorCode implements DomainErrorCode {

    SUPPLIER_ORDER_NOT_FOUND(ErrorCode.NOT_FOUND, "공급처 발주서를 찾을 수 없습니다."),
    DUPLICATE_SUPPLIER_ORDER_NO(ErrorCode.CONFLICT, "이미 사용 중인 공급처 발주서 번호입니다. 다시 시도해주세요.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    SupplierOrderErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
