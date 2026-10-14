package com.kb.wms.inbound.application.port.out;

import java.util.Collection;

import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

/**
 * 진행 중 업무 존재 여부 조회 아웃바운드 포트(읽기 전용). 상태 집합은 호출하는 서비스가 정해 넘긴다.
 */
public interface InboundUsageQueryPort {

    boolean existsInboundBySkuIds(Collection<Long> skuIds, Collection<InboundStatus> statuses);

    boolean existsInboundByWarehouse(Long warehouseId, Collection<InboundStatus> statuses);

    boolean existsPurchaseOrderBySkuIds(Collection<Long> skuIds, Collection<PurchaseOrderStatus> statuses);

    boolean existsPurchaseOrderByWarehouse(Long warehouseId, Collection<PurchaseOrderStatus> statuses);
}
