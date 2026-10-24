package com.kb.wms.dashboard.application.port.out;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.kb.wms.dashboard.application.port.in.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.result.TopOutboundSku;
import com.kb.wms.dashboard.application.port.in.result.WarehouseDashboardRow;

/**
 * 대시보드 집계 조회 포트(읽기 전용). 다른 도메인 테이블을 ID로 조인해 집계 결과를 바로 돌려준다(ADR-033).
 *
 * <p>입출고 집계의 진행중/완료 기준은 {@code docs/api/dashboard.md}를 따른다.
 * {@code warehouseId}가 null이면 전 창고를 대상으로 한다. 월 경계는 서버 시간대(KST) 기준이다.
 */
public interface DashboardQueryRepository {

    PendingCounts findPendingCounts();

    /**
     * @param expiringOnOrBefore 이 날짜(당일 포함)까지 유통기한이 도래하는 로트를 임박으로 센다
     */
    InventoryTotals findInventoryTotals(LocalDate expiringOnOrBefore);

    /** 입고 완료(COMPLETED) 중 검수 완료 시각이 해당 월인 건. */
    FlowCount findInboundCompleted(YearMonth month, Long warehouseId);

    /** 입고 진행중(INSPECTING). 월과 무관한 현재 시점 전체다. */
    FlowCount findInboundInProgress(Long warehouseId);

    /** 출고 완료(DELIVERED) 중 배송 완료 시각이 해당 월인 건. */
    FlowCount findOutboundCompleted(YearMonth month, Long warehouseId);

    /** 출고 진행중(PICKING·PICKED·SHIPPED). 월과 무관한 현재 시점 전체다. */
    FlowCount findOutboundInProgress(Long warehouseId);

    /**
     * 요청 시각이 해당 월인 지점 발주의 상태별 건수. 건수가 있는 상태만 담는다(0건 상태 채우기는 서비스가 한다).
     */
    Map<String, Long> countStoreOrdersByStatus(YearMonth month);

    /** 활성 창고 전체의 창고별 비교, 창고 ID 오름차순. */
    List<WarehouseDashboardRow> findWarehouseRows(YearMonth month);

    /**
     * 월별 입출고 완료 수량. 완료 건이 있는 달만 담고(빈 달 채우기는 서비스가 한다),
     * 진행중 수량은 null로 둔다.
     */
    List<MonthlyFlow> findMonthlyCompleted(YearMonth from, YearMonth to, Long warehouseId);

    /** 해당 월 출고 완료 수량 상위 SKU. 수량 내림차순, 같으면 SKU ID 오름차순. */
    List<TopOutboundSku> findTopOutboundSkus(YearMonth month, Long warehouseId, int limit);
}
