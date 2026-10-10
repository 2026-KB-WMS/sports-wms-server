package com.kb.wms.inventory.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 재고 도메인 특수 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 담는다.
 * 재고·로트 미존재(404)는 이 도메인 전용 코드를 쓴다. 조회 필터 대상(SKU·창고·구역·공급처) 미존재 404는
 * 다른 도메인 에러 코드 enum을 import하지 않기 위해(ADR-005) 해당 도메인과 같은 코드명으로 여기에 둔다.
 */
@Getter
public enum InventoryErrorCode implements DomainErrorCode {

    INVENTORY_NOT_FOUND(ErrorCode.NOT_FOUND, "재고를 찾을 수 없습니다."),
    LOT_NOT_FOUND(ErrorCode.NOT_FOUND, "로트를 찾을 수 없습니다."),
    SKU_NOT_FOUND(ErrorCode.NOT_FOUND, "SKU를 찾을 수 없습니다."),
    WAREHOUSE_NOT_FOUND(ErrorCode.NOT_FOUND, "창고를 찾을 수 없습니다."),
    SECTION_NOT_FOUND(ErrorCode.NOT_FOUND, "구역을 찾을 수 없습니다."),
    SUPPLIER_NOT_FOUND(ErrorCode.NOT_FOUND, "공급처를 찾을 수 없습니다."),
    STALE_QUANTITY(ErrorCode.CONFLICT, "조회 이후 재고 수량이 변경되었습니다. 현재 수량을 다시 확인해주세요."),
    BELOW_ALLOCATED_QUANTITY(ErrorCode.CONFLICT, "보유 수량은 할당 수량보다 작게 조정할 수 없습니다."),
    INSUFFICIENT_STOCK(ErrorCode.CONFLICT, "가용 재고가 부족합니다."),
    LOT_NOT_AVAILABLE(ErrorCode.CONFLICT, "가용 상태가 아닌 로트입니다(만료·격리·폐기)."),
    SKU_NOT_ACTIVE(ErrorCode.CONFLICT, "비활성 SKU는 입고·할당할 수 없습니다."),
    INVALID_LOT_STATUS_TRANSITION(ErrorCode.CONFLICT, "허용되지 않는 로트 상태 변경입니다."),
    LOT_HAS_ALLOCATION(ErrorCode.CONFLICT, "할당 수량이 남아 있는 로트는 격리할 수 없습니다."),
    LOT_HAS_STOCK(ErrorCode.CONFLICT, "보유 또는 할당 수량이 남아 있는 로트는 폐기할 수 없습니다."),
    LOT_UNIT_COST_MISMATCH(ErrorCode.CONFLICT, "기존 로트의 원가와 입고 단가가 다릅니다."),
    LOT_DATE_MISMATCH(ErrorCode.CONFLICT, "기존 로트의 제조일·유통기한과 요청 값이 다릅니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    InventoryErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
