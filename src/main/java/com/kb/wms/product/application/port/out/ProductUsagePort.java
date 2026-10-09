package com.kb.wms.product.application.port.out;

/**
 * 상품의 SKU를 참조하는 재고·진행 중 업무 존재 여부 아웃바운드 포트(재고·입고·발주·출고·지점 발주 도메인, 읽기 전용).
 * 상품 비활성화 전에 아직 쓰이고 있는지 확인한다(ADR-007: 조회 전용 도메인 간 조인).
 */
public interface ProductUsagePort {

    /** 상품의 어느 SKU든 보유·할당 수량이 0보다 큰 재고 행이 있으면 true. */
    boolean hasStock(Long productId);

    /** 종결되지 않은 입고(ARRIVED·INSPECTING)의 항목에 이 상품의 SKU가 있으면 true. */
    boolean hasInProgressInbounds(Long productId);

    /** 종결되지 않은 창고 발주(REQUESTED·CONFIRMED)의 항목에 이 상품의 SKU가 있으면 true. */
    boolean hasInProgressPurchaseOrders(Long productId);

    /**
     * 종결되지 않은 출고(READY·PICKING·PICKED·SHIPPED)가 있고 그 지점 발주에 이 상품의 SKU 항목이 있으면 true.
     * 출고 항목은 SKU를 직접 갖지 않아 출고가 속한 지점 발주의 항목으로 판단한다.
     */
    boolean hasInProgressOutbounds(Long productId);

    /** 종결되지 않은 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)의 항목에 이 상품의 SKU가 있으면 true. */
    boolean hasInProgressStoreOrders(Long productId);
}
