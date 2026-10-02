package com.kb.wms.inbound.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 입고 테이블의 유니크 제약 이름 → 도메인 에러 코드 (V9__create_inbound_tables.sql 기준).
 * 입고 번호는 서버가 채번하므로 동시 등록으로 번호가 겹친 경우에만 DB 제약이 409를 응답한다.
 * 검수 항목 중복((입고, 발주 항목, 로트))은 서비스가 먼저 400으로 거른다.
 */
@Component
public class InboundConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of("uk_inbound_no", InboundErrorCode.DUPLICATE_INBOUND_NO);
    }
}
