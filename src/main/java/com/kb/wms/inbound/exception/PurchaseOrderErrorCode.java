package com.kb.wms.inbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 발주 도메인 전용 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 쓴다.
 * 상태 충돌(409)은 공통 코드(CONFLICT)를 쓴다.
 */
@Getter
public enum PurchaseOrderErrorCode implements DomainErrorCode {

    PURCHASE_ORDER_NOT_FOUND(ErrorCode.NOT_FOUND, "창고 발주를 찾을 수 없습니다."),
    PURCHASE_PRICE_MISSING(ErrorCode.CONFLICT, "SKU에 매입 단가가 없어 발주 단가를 확정할 수 없습니다."),
    SUPPLIER_INACTIVE(ErrorCode.CONFLICT, "발주의 공급처가 비활성 상태입니다."),
    DUPLICATE_PURCHASE_ORDER_NO(ErrorCode.CONFLICT, "이미 사용 중인 발주 번호입니다. 다시 시도해주세요.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    PurchaseOrderErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
