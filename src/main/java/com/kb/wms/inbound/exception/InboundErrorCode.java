package com.kb.wms.inbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * 입고 도메인 고유 에러 코드.
 * WMS API 명세(Notion)의 각 엔드포인트 에러 표에 정의된 도메인별 error_code 값을 따른다.
 * 일반적인 상태 충돌(409)은 공통 코드(CONFLICT)를 쓴다.
 */
@Getter
public enum InboundErrorCode implements DomainErrorCode {

    INBOUND_NOT_FOUND(ErrorCode.NOT_FOUND, "입고를 찾을 수 없습니다."),
    INBOUND_IN_PROGRESS(ErrorCode.CONFLICT, "같은 발주에 아직 완료되지 않은 입고가 있습니다. 진행 중인 입고를 먼저 완료하거나 취소해주세요."),
    DUPLICATE_INBOUND_NO(ErrorCode.CONFLICT, "이미 사용 중인 입고 번호입니다. 다시 시도해주세요."),
    INBOUND_HAS_NO_LINES(ErrorCode.CONFLICT, "저장된 검수 항목이 없습니다. 검수 항목을 먼저 등록해주세요."),
    SECTION_NOT_ASSIGNED(ErrorCode.CONFLICT, "합격 수량이 있는 항목에는 합격품 구역을, 불량 수량이 있는 항목에는 불량품 구역을 지정해야 합니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    InboundErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
