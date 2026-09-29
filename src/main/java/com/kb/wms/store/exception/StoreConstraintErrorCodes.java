package com.kb.wms.store.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 지점 도메인 유니크 제약 조건 → 오류 코드 (V6__create_store_tables.sql 기준).
 */
@Component
public class StoreConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of(
                "uk_store_code", StoreErrorCode.DUPLICATE_STORE_CODE,
                "uk_store_member_store_user", StoreErrorCode.ALREADY_ASSIGNED);
    }
}
