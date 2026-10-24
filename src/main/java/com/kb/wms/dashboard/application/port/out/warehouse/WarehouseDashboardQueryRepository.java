package com.kb.wms.dashboard.application.port.out.warehouse;

import java.time.LocalDate;

import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;

/**
 * 창고 관리자 홈 집계 조회 포트(읽기 전용). 한 창고만 대상으로 하며 다른 도메인 테이블을 ID로 조인해
 * 집계 결과를 바로 돌려준다(ADR-033). 담당 창고 검사는 서비스가 한다.
 */
public interface WarehouseDashboardQueryRepository {

    /** 입고 작업 대기: 검수 전(ARRIVED)과 검수 중(INSPECTING). 현재 시점 전체다. */
    InboundWorkCounts findInboundWorkCounts(Long warehouseId);

    /** 출고 작업 대기: 피킹 전(READY), 작업 중(PICKING·PICKED), 배송 중(SHIPPED). 현재 시점 전체다. */
    OutboundWorkCounts findOutboundWorkCounts(Long warehouseId);

    /**
     * 수용량 사용 현황. 사용량은 활성 구역 전체의 적치량 합, 수용량은 활성 최상위 구역의 수용량 합이다
     * (하위 구역 수용량은 상위에 포함되고, 적치량은 구역 단위로만 늘어 상위로 합산되지 않는다).
     */
    CapacityUsage findCapacityUsage(Long warehouseId);

    /**
     * 재고 주의 항목.
     *
     * @param expiringOnOrBefore 이 날짜(당일 포함)까지 유통기한이 도래하는 로트를 임박으로 센다
     * @param lowStockLimit      부족 폭이 큰 SKU를 몇 개까지 담을지
     */
    WarehouseInventoryAlerts findInventoryAlerts(Long warehouseId, LocalDate expiringOnOrBefore, int lowStockLimit);
}
