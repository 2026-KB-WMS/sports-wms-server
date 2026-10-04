package com.kb.wms.storeorder.application.port.in.result;

import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;

/**
 * 지점 발주 목록 한 행 (GET /api/v1/orders). 조회 쿼리 결과에 출고 연동 포트로 채운 최근 출고 상태와 진행 단계를 더한 것이다.
 *
 * @param latestOutboundStatus 가장 최근 출고의 상태. 출고가 없으면 null
 * @param progressStage        점주용 진행 단계(파생 값)
 */
public record StoreOrderListItem(
        StoreOrderSummary summary,
        StoreOrderOutboundStatus latestOutboundStatus,
        StoreOrderProgressStage progressStage
) {
}
