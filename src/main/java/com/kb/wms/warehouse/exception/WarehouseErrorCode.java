package com.kb.wms.warehouse.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 창고 도메인 특수 오류 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 오류 표에 정의된 도메인 전용 error_code 값을 담는다.
 */
@Getter
public enum WarehouseErrorCode implements DomainErrorCode {

    WAREHOUSE_NOT_FOUND(ErrorCode.NOT_FOUND, "창고를 찾을 수 없습니다."),
    DUPLICATE_WAREHOUSE_CODE(ErrorCode.CONFLICT, "이미 사용 중인 창고 코드입니다."),
    SECTION_NOT_FOUND(ErrorCode.NOT_FOUND, "구역을 찾을 수 없습니다."),
    PARENT_SECTION_NOT_FOUND(ErrorCode.NOT_FOUND, "상위 구역을 찾을 수 없습니다."),
    DUPLICATE_SECTION_CODE(ErrorCode.CONFLICT, "해당 창고에 이미 존재하는 구역 코드입니다."),
    CAPACITY_BELOW_USAGE(ErrorCode.CONFLICT, "수용량은 현재 사용 용량보다 작게 설정할 수 없습니다."),
    SECTION_CAPACITY_EXCEEDED(ErrorCode.CONFLICT, "구역의 수용량을 초과합니다."),
    SECTION_INACTIVE(ErrorCode.CONFLICT, "비활성 구역에는 재고를 적치할 수 없습니다."),
    WAREHOUSE_INACTIVE(ErrorCode.CONFLICT, "비활성 창고의 구역에는 재고를 적치할 수 없습니다."),
    SECTION_HAS_INVENTORY(ErrorCode.CONFLICT, "재고가 남아 있는 구역은 비활성화할 수 없습니다."),
    SECTION_HAS_CHILDREN(ErrorCode.CONFLICT, "활성 상태의 하위 구역이 있어 비활성화할 수 없습니다."),
    WAREHOUSE_IN_USE(ErrorCode.CONFLICT, "재고가 남아 있거나 진행 중인 업무가 있어 비활성화할 수 없습니다."),
    MEMBER_NOT_FOUND(ErrorCode.NOT_FOUND, "창고 소속 정보를 찾을 수 없습니다."),
    ALREADY_ASSIGNED(ErrorCode.CONFLICT, "이미 해당 창고에 배정된 사용자입니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    WarehouseErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
