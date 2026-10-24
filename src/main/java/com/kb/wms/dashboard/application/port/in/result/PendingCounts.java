package com.kb.wms.dashboard.application.port.in.result;

/**
 * 본사가 지금 처리해야 할 대기 건수. 기간 필터 없이 현재 시점 전체를 센다.
 *
 * @param storeOrderRequestedCount           승인 대기 지점 발주(REQUESTED)
 * @param purchaseOrderRequestedCount        확정 대기 창고 발주(REQUESTED)
 * @param unassignedApprovedStoreOrderCount  창고 배정 전 지점 발주(APPROVED)
 */
public record PendingCounts(
        Long storeOrderRequestedCount,
        Long purchaseOrderRequestedCount,
        Long unassignedApprovedStoreOrderCount
) {
}
