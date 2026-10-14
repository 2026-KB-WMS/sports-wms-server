package com.kb.wms.outbound.application.service;

import java.util.Collection;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.outbound.application.port.in.OutboundUsageUseCase;
import com.kb.wms.outbound.application.port.out.OutboundUsageQueryPort;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundUsageService implements OutboundUsageUseCase {

    /** 종결되지 않은 출고(READY·PICKING·PICKED·SHIPPED). 나머지 상태는 비활성화를 막지 않는다. */
    private static final Set<OutboundStatus> OUTBOUND_IN_PROGRESS = Set.of(
            OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED);

    private final OutboundUsageQueryPort queryPort;

    @Override
    public boolean hasInProgressOutboundForSkus(Collection<Long> skuIds) {
        return !skuIds.isEmpty() && queryPort.existsOutboundBySkuIds(skuIds, OUTBOUND_IN_PROGRESS);
    }

    @Override
    public boolean hasInProgressOutboundForWarehouse(Long warehouseId) {
        return queryPort.existsOutboundByWarehouse(warehouseId, OUTBOUND_IN_PROGRESS);
    }
}
