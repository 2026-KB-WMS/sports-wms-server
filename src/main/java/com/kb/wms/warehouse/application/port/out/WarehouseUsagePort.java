package com.kb.wms.warehouse.application.port.out;

/**
 * 창고를 참조하는 진행 중 업무 존재 여부 아웃바운드 포트(입고·발주·출고·지점 발주 도메인, 읽기 전용).
 * 창고 비활성화 전에 아직 종결되지 않은 업무가 남아 있는지 확인한다(ADR-007: 조회 전용 도메인 간 조인).
 */
public interface WarehouseUsagePort {

    /** 종결되지 않은 입고(ARRIVED·INSPECTING)가 있으면 true. COMPLETED·CANCELED는 무시한다. */
    boolean hasInProgressInbounds(Long warehouseId);

    /** 종결되지 않은 창고 발주(REQUESTED·CONFIRMED)가 있으면 true. COMPLETED·CANCELED는 무시한다. */
    boolean hasInProgressPurchaseOrders(Long warehouseId);

    /**
     * 종결되지 않은 출고(READY·PICKING·PICKED·SHIPPED)가 있으면 true. DELIVERED·CANCELED는 무시한다.
     * 출고는 창고를 직접 갖지 않아 배정된 지점 발주의 창고로 판단한다.
     */
    boolean hasInProgressOutbounds(Long warehouseId);

    /** 이 창고에 배정된 종결되지 않은 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)가 있으면 true. */
    boolean hasInProgressStoreOrders(Long warehouseId);

    /**
     * 구역을 참조하는 행이 있으면 true. 재고 로트 행(수량 0 포함)과 입고 검수 항목의 합격·불량 구역 지정을 본다.
     * 입고 상태와 무관하게 세므로, 완료된 입고가 참조한 구역도 삭제할 수 없다. 구역 삭제 전 확인에 쓴다.
     */
    boolean isSectionReferenced(Long sectionId);
}
