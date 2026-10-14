package com.kb.wms.storeorder.application.service;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.storeorder.application.port.in.StoreOrderUsageUseCase;
import com.kb.wms.storeorder.application.port.out.StoreOrderUsageQueryPort;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreOrderUsageService implements StoreOrderUsageUseCase {

    /** 종결되지 않은 지점 발주(REQUESTED·APPROVED·ASSIGNED·ON_HOLD). 나머지 상태는 비활성화를 막지 않는다. */
    private static final Set<StoreOrderStatus> STORE_ORDER_IN_PROGRESS = Set.of(
            StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED, StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD);

    private final StoreOrderUsageQueryPort queryPort;

    @Override
    public boolean hasInProgressStoreOrderForSkus(Collection<Long> skuIds) {
        return !skuIds.isEmpty() && queryPort.existsStoreOrderBySkuIds(skuIds, STORE_ORDER_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressStoreOrderForWarehouse(Long warehouseId) {
        return queryPort.existsStoreOrderByWarehouse(warehouseId, STORE_ORDER_IN_PROGRESS);
    }
}
