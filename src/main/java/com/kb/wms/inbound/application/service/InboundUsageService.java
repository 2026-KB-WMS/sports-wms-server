package com.kb.wms.inbound.application.service;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.application.port.in.InboundUsageUseCase;
import com.kb.wms.inbound.application.port.out.InboundUsageQueryPort;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundUsageService implements InboundUsageUseCase {

    /** 종결되지 않은 입고(ARRIVED·INSPECTING). 나머지 상태는 비활성화를 막지 않는다. */
    private static final Set<InboundStatus> INBOUND_IN_PROGRESS = Set.of(
            InboundStatus.ARRIVED, InboundStatus.INSPECTING);

    /** 종결되지 않은 창고 발주(REQUESTED·CONFIRMED). 나머지 상태는 비활성화를 막지 않는다. */
    private static final Set<PurchaseOrderStatus> PURCHASE_ORDER_IN_PROGRESS = Set.of(
            PurchaseOrderStatus.REQUESTED, PurchaseOrderStatus.CONFIRMED);

    private final InboundUsageQueryPort queryPort;

    @Override
    public boolean hasInProgressInboundForSkus(Collection<Long> skuIds) {
        return !skuIds.isEmpty() && queryPort.existsInboundBySkuIds(skuIds, INBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressInboundForWarehouse(Long warehouseId) {
        return queryPort.existsInboundByWarehouse(warehouseId, INBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressPurchaseOrderForSkus(Collection<Long> skuIds) {
        return !skuIds.isEmpty() && queryPort.existsPurchaseOrderBySkuIds(skuIds, PURCHASE_ORDER_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressPurchaseOrderForWarehouse(Long warehouseId) {
        return queryPort.existsPurchaseOrderByWarehouse(warehouseId, PURCHASE_ORDER_IN_PROGRESS);
    }
}
