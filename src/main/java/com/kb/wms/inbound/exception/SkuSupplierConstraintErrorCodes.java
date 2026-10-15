package com.kb.wms.inbound.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * sku_supplier 테이블의 유니크 제약 이름 → 도메인 오류 코드 (V1__baseline.sql 기준).
 * 서비스가 먼저 존재 여부를 확인하므로, 동시 등록으로 같은 쌍이 들어온 경우에만 DB 제약이 409로 응답한다.
 */
@Component
public class SkuSupplierConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of("uk_sku_supplier_sku_supplier", SkuSupplierErrorCode.SKU_SUPPLIER_ALREADY_MAPPED);
    }
}
