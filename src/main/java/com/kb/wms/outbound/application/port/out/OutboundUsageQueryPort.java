package com.kb.wms.outbound.application.port.out;

import java.util.Collection;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 진행 중 업무 존재 여부 조회 아웃바운드 포트(읽기 전용). 상태 집합은 호출하는 서비스가 정해 넘긴다.
 */
public interface OutboundUsageQueryPort {

    boolean existsOutboundBySkuIds(Collection<Long> skuIds, Collection<OutboundStatus> statuses);

    boolean existsOutboundByWarehouse(Long warehouseId, Collection<OutboundStatus> statuses);
}
