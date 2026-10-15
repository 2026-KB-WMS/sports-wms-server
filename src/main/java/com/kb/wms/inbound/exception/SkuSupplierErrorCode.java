package com.kb.wms.inbound.exception;

import com.kb.wms.common.exception.DomainErrorCode;
import com.kb.wms.common.exception.ErrorCode;

import lombok.Getter;

/**
 * SKU↔공급처 매핑 전용 오류 코드. SKU·공급처의 404는 각 도메인 코드(SKU_NOT_FOUND, SUPPLIER_NOT_FOUND)를,
 * 비활성 공급처의 409는 SUPPLIER_INACTIVE를 쓴다.
 */
@Getter
public enum SkuSupplierErrorCode implements DomainErrorCode {

    SKU_SUPPLIER_NOT_FOUND(ErrorCode.NOT_FOUND, "SKU에 매핑된 공급처가 아닙니다."),
    SKU_SUPPLIER_ALREADY_MAPPED(ErrorCode.CONFLICT, "이미 SKU에 매핑된 공급처입니다.");

    private final ErrorCode errorCode;
    private final String defaultMessage;

    SkuSupplierErrorCode(ErrorCode errorCode, String defaultMessage) {
        this.errorCode = errorCode;
        this.defaultMessage = defaultMessage;
    }
}
