package com.kb.wms.warehouse.application.port.out;

/**
 * 재고 존재 여부 아웃바운드 포트(재고 도메인). 구역·창고 비활성화 전에 남은 재고(보유·할당)를 확인한다.
 */
public interface StockPresencePort {

    boolean hasStockInSection(Long sectionId);

    boolean hasStockInWarehouse(Long warehouseId);
}
