package com.kb.wms.inventory.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 재고 도메인 특수 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 담는다.
 * 재고·로트 미존재는 명세상 공통 NOT_FOUND를 쓰므로 여기에 두지 않는다.
 */
@Getter
public enum InventoryErrorCode implements DomainErrorCode {

    STALE_QUANTITY(ErrorCode.CONFLICT, "조회 이후 재고 수량이 변경되었습니다. 현재 수량을 다시 확인해주세요."),
    BELOW_ALLOCATED_QUANTITY(ErrorCode.CONFLICT, "보유 수량은 할당 수량보다 작게 조정할 수 없습니다."),
    INSUFFICIENT_STOCK(ErrorCode.CONFLICT, "가용 재고가 부족합니다."),
    LOT_NOT_AVAILABLE(ErrorCode.CONFLICT, "가용 상태가 아닌 로트입니다(만료·격리·폐기)."),
    LOT_UNIT_COST_MISMATCH(ErrorCode.CONFLICT, "기존 로트의 원가와 입고 단가가 다릅니다."),
    LOT_DATE_MISMATCH(ErrorCode.CONFLICT, "기존 로트의 제조일·유통기한과 요청 값이 다릅니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    InventoryErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
