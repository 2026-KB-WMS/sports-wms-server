package com.kb.wms.outbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 출고·재고 할당 도메인의 사용자 노출 오류 코드.
 * 상태 충돌(409)의 일반 코드는 CONFLICT를 쓰고, 이 도메인에서만 의미가 있는 충돌과 404만 여기에 둔다.
 * 재고 도메인이 거절하는 LOT_NOT_AVAILABLE·SKU_NOT_ACTIVE 등은 재고 쪽 코드 그대로 전달한다.
 */
@Getter
public enum OutboundErrorCode implements DomainErrorCode {

    ALLOCATION_NOT_FOUND(ErrorCode.NOT_FOUND, "재고 할당을 찾을 수 없습니다."),
    OUTBOUND_NOT_FOUND(ErrorCode.NOT_FOUND, "출고를 찾을 수 없습니다."),
    ALREADY_ALLOCATED(ErrorCode.CONFLICT, "할당할 잔여 수량이 없습니다."),
    INSUFFICIENT_STOCK(ErrorCode.CONFLICT, "가용 재고가 부족해 할당할 수 없습니다."),
    ALLOCATION_IN_OUTBOUND(ErrorCode.CONFLICT, "출고에 연결된 재고 할당은 해제할 수 없습니다."),
    ORDER_NOT_ASSIGNED(ErrorCode.CONFLICT, "배정 상태가 아닌 발주입니다."),
    ALLOCATION_NOT_ACTIVE(ErrorCode.CONFLICT, "할당 상태가 아닌 재고 할당이 포함되어 있습니다."),
    NO_ALLOCATION(ErrorCode.CONFLICT, "출고에 묶을 재고 할당이 없습니다."),
    NOTHING_PICKED(ErrorCode.CONFLICT, "피킹한 수량이 없어 피킹을 완료할 수 없습니다."),
    SUPPLY_PRICE_MISSING(ErrorCode.CONFLICT, "발주 항목과 SKU 모두 공급 단가가 없어 금액을 확정할 수 없습니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    OutboundErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}