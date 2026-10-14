package com.kb.wms.inbound.application.port.in;

import java.util.Collection;

/**
 * 입고·창고 발주 진행 중 업무 존재 여부 조회 인바운드 포트(다른 도메인용 읽기 전용).
 *
 * <p>상품·SKU·창고 비활성화 가드처럼 다른 도메인이 "아직 진행 중인 업무가 있는지"만 확인할 때 쓴다.
 * 어떤 상태를 진행 중으로 볼지는 이 도메인이 정하고, 호출하는 쪽에는 결과만 돌려준다.
 */
public interface InboundUsageUseCase {

    /** SKU 중 하나라도 종결되지 않은 입고(ARRIVED·INSPECTING)의 항목에 쓰이고 있으면 true. 목록이 비어 있으면 false. */
    boolean hasInProgressInboundForSkus(Collection<Long> skuIds);

    /** 이 창고에 속한 종결되지 않은 입고(ARRIVED·INSPECTING)가 있으면 true. */
    boolean hasInProgressInboundForWarehouse(Long warehouseId);

    /** SKU 중 하나라도 종결되지 않은 창고 발주(REQUESTED·CONFIRMED)의 항목에 쓰이고 있으면 true. 목록이 비어 있으면 false. */
    boolean hasInProgressPurchaseOrderForSkus(Collection<Long> skuIds);

    /** 이 창고에 속한 종결되지 않은 창고 발주(REQUESTED·CONFIRMED)가 있으면 true. */
    boolean hasInProgressPurchaseOrderForWarehouse(Long warehouseId);
}
