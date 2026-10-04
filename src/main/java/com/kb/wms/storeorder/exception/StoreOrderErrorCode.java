package com.kb.wms.storeorder.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 지점 발주 도메인 전용 오류 코드.
 * 상태 충돌(409)은 공통 코드(CONFLICT)를 쓴다. 승인·취소·배정 등 후속 서브이슈에서 필요한 코드를 이 enum에 추가한다.
 */
@Getter
public enum StoreOrderErrorCode implements DomainErrorCode {

    STORE_ORDER_NOT_FOUND(ErrorCode.NOT_FOUND, "지점 발주를 찾을 수 없습니다."),
    SUPPLY_PRICE_MISSING(ErrorCode.CONFLICT, "SKU에 공급 단가가 없어 발주 단가를 확정할 수 없습니다."),
    DUPLICATE_STORE_ORDER_NO(ErrorCode.CONFLICT, "이미 사용 중인 주문 번호입니다. 다시 시도해주세요."),
    ORDER_IN_PICKING(ErrorCode.CONFLICT, "피킹이 시작된 출고가 있어 발주를 취소할 수 없습니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    StoreOrderErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
