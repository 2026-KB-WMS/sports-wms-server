package com.kb.wms.warehouse.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 창고 도메인 유니크 제약 조건 → 오류 코드 (V3__create_warehouse_tables.sql 기준).
 */
@Component
public class WarehouseConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of(
                "uk_warehouse_code", WarehouseErrorCode.DUPLICATE_WAREHOUSE_CODE,
                "uk_warehouse_section_code", WarehouseErrorCode.DUPLICATE_SECTION_CODE,
                "uk_warehouse_member_warehouse_user", WarehouseErrorCode.ALREADY_ASSIGNED);
    }
}
