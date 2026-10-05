package com.kb.wms.outbound.exception;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.kb.wms.common.exception.ConstraintErrorCodeProvider;
import com.kb.wms.common.exception.DomainErrorCode;

/**
 * 출고 테이블의 유니크 제약 이름 → 도메인 오류 코드 (V13__create_outbound_tables.sql 기준).
 * 출고 번호는 서버가 채번하므로 동시 생성으로 같은 번호가 만들어진 경우에만 DB 제약이 409로 응답한다.
 */
@Component
public class OutboundConstraintErrorCodes implements ConstraintErrorCodeProvider {

    @Override
    public Map<String, DomainErrorCode> constraintErrorCodes() {
        return Map.of("uk_outbound_no", OutboundErrorCode.DUPLICATE_OUTBOUND_NO);
    }
}
