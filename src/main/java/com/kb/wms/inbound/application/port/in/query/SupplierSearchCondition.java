package com.kb.wms.inbound.application.port.in.query;

/**
 * GET /api/v1/suppliers 검색 조건. null인 조건은 무시한다.
 *
 * @param keyword  공급처명·공급처 코드·담당자명 부분 일치
 * @param isActive 거래 상태 필터 (null이면 전체). 창고 관리자는 서비스에서 항상 true로 강제한다.
 */
public record SupplierSearchCondition(
        String keyword,
        Boolean isActive
) {
}
