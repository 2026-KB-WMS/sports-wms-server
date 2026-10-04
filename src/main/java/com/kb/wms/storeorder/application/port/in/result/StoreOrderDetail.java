package com.kb.wms.storeorder.application.port.in.result;

import com.kb.wms.storeorder.domain.enums.StoreOrderProgressStage;

/**
 * 지점 발주 헤더 단건 (GET /api/v1/orders/{orderId}). 조회 쿼리 결과에 상태 사유와 진행 단계를 더한 것이다.
 *
 * @param statusReason  현재 상태가 REJECTED·CANCELED·ON_HOLD일 때 StatusHistory에 기록된 사유. 그 외에는 null
 * @param progressStage 점주용 진행 단계(파생 값)
 */
public record StoreOrderDetail(
        StoreOrderView view,
        String statusReason,
        StoreOrderProgressStage progressStage
) {
}
