package com.kb.wms.inbound.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 공급처 도메인 유니크 제약 조건 → 오류 코드 (V7__create_supplier_table.sql 기준).
 * 사전 조회와 별개로 동시 등록 시 DB 제약 위반도 409로 매핑한다.
 */
@Component
public class SupplierConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of("uk_supplier_code", SupplierErrorCode.DUPLICATE_SUPPLIER_CODE);
    }
}
