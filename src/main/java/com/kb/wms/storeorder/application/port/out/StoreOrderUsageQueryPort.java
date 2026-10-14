package com.kb.wms.storeorder.application.port.out;

import java.util.Collection;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 진행 중 업무 존재 여부 조회 아웃바운드 포트(읽기 전용). 상태 집합은 호출하는 서비스가 정해 넘긴다.
 */
public interface StoreOrderUsageQueryPort {

    boolean existsStoreOrderBySkuIds(Collection<Long> skuIds, Collection<StoreOrderStatus> statuses);

    boolean existsStoreOrderByWarehouse(Long warehouseId, Collection<StoreOrderStatus> statuses);
}
