package com.kb.wms.inbound.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 발주 테이블의 유니크 제약 이름 → 도메인 오류 코드 (V8__create_purchase_order_tables.sql 기준).
 * 발주 번호는 서버가 채번하므로 동시 등록으로 같은 번호가 만들어진 경우에만 DB 제약이 409로 응답한다.
 * 같은 발주 안의 SKU 중복(uk_purchase_order_line_order_sku)은 서비스가 먼저 400으로 거른다.
 */
@Component
public class PurchaseOrderConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of("uk_purchase_order_no", PurchaseOrderErrorCode.DUPLICATE_PURCHASE_ORDER_NO);
    }
}
