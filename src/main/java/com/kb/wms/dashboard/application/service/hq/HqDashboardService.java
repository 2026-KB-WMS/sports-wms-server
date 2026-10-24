package com.kb.wms.dashboard.application.service.hq;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.dashboard.application.port.in.hq.HqDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.hq.query.HqSummaryQuery;
import com.kb.wms.dashboard.application.port.in.hq.query.HqTrendsQuery;
import com.kb.wms.dashboard.application.port.in.hq.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.hq.result.FlowTotals;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardSummary;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardTrends;
import com.kb.wms.dashboard.application.port.in.hq.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.hq.result.StoreOrderStats;
import com.kb.wms.dashboard.application.port.out.hq.HqDashboardQueryRepository;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HqDashboardService implements HqDashboardUseCase {

    static final int DEFAULT_EXPIRING_WITHIN_DAYS = 30;
    static final int MAX_EXPIRING_WITHIN_DAYS = 365;
    static final int DEFAULT_TREND_MONTHS = 6;
    static final int MAX_TREND_MONTHS = 12;
    static final int TOP_SKU_LIMIT = 5;

    /**
     * 지점 발주 상태 이름. 다른 도메인의 enum을 import할 수 없어 이름으로 두며,
     * 0건인 상태도 응답에 채우려면 StoreOrderStatus와 같은 순서로 맞춰야 한다.
     */
    private static final List<String> STORE_ORDER_STATUSES = List.of(
            "REQUESTED", "APPROVED", "ASSIGNED", "ON_HOLD", "COMPLETED", "CANCELED", "REJECTED");

    private final HqDashboardQueryRepository dashboardQueryRepository;
    private final WarehouseUseCase warehouseUseCase;

    @Override
    public HqDashboardSummary getSummary(HqSummaryQuery query) {
        YearMonth month = query.month() != null ? query.month() : YearMonth.now();
        int expiringWithinDays = query.expiringWithinDays() != null
                ? query.expiringWithinDays() : DEFAULT_EXPIRING_WITHIN_DAYS;
        if (expiringWithinDays < 1 || expiringWithinDays > MAX_EXPIRING_WITHIN_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "expiringWithinDays는 1 이상 " + MAX_EXPIRING_WITHIN_DAYS + " 이하여야 합니다.");
        }

        YearMonth previous = month.minusMonths(1);
        return new HqDashboardSummary(
                month,
                dashboardQueryRepository.findPendingCounts(),
                dashboardQueryRepository.findInventoryTotals(LocalDate.now().plusDays(expiringWithinDays)),
                new FlowTotals(
                        dashboardQueryRepository.findInboundCompleted(month, null),
                        dashboardQueryRepository.findInboundInProgress(null),
                        dashboardQueryRepository.findInboundCompleted(previous, null)),
                new FlowTotals(
                        dashboardQueryRepository.findOutboundCompleted(month, null),
                        dashboardQueryRepository.findOutboundInProgress(null),
                        dashboardQueryRepository.findOutboundCompleted(previous, null)),
                storeOrderStats(dashboardQueryRepository.countStoreOrdersByStatus(month)),
                dashboardQueryRepository.findWarehouseRows(month));
    }

    @Override
    public HqDashboardTrends getTrends(HqTrendsQuery query) {
        int months = query.months() != null ? query.months() : DEFAULT_TREND_MONTHS;
        if (months < 1 || months > MAX_TREND_MONTHS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "months는 1 이상 " + MAX_TREND_MONTHS + " 이하여야 합니다.");
        }
        Long warehouseId = query.warehouseId();
        if (warehouseId != null) {
            warehouseUseCase.getWarehouseView(warehouseId);
        }

        YearMonth current = YearMonth.now();
        YearMonth from = current.minusMonths(months - 1L);
        Map<YearMonth, MonthlyFlow> completedByMonth = new HashMap<>();
        for (MonthlyFlow flow : dashboardQueryRepository.findMonthlyCompleted(from, current, warehouseId)) {
            completedByMonth.put(flow.month(), flow);
        }
        FlowCount inboundInProgress = dashboardQueryRepository.findInboundInProgress(warehouseId);
        FlowCount outboundInProgress = dashboardQueryRepository.findOutboundInProgress(warehouseId);

        List<MonthlyFlow> items = new ArrayList<>();
        for (YearMonth month = from; !month.isAfter(current); month = month.plusMonths(1)) {
            MonthlyFlow completed = completedByMonth.get(month);
            boolean isCurrent = month.equals(current);
            items.add(new MonthlyFlow(
                    month,
                    completed != null ? completed.inboundCompletedQuantity() : 0L,
                    completed != null ? completed.outboundCompletedQuantity() : 0L,
                    isCurrent ? inboundInProgress.quantity() : null,
                    isCurrent ? outboundInProgress.quantity() : null));
        }
        return new HqDashboardTrends(items,
                dashboardQueryRepository.findTopOutboundSkus(current, warehouseId, TOP_SKU_LIMIT));
    }

    private StoreOrderStats storeOrderStats(Map<String, Long> counted) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : STORE_ORDER_STATUSES) {
            byStatus.put(status, counted.getOrDefault(status, 0L));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long denominator = total - byStatus.get("CANCELED") - byStatus.get("REJECTED");
        BigDecimal completionRate = denominator == 0 ? null
                : BigDecimal.valueOf(byStatus.get("COMPLETED")).divide(BigDecimal.valueOf(denominator), 2,
                        RoundingMode.HALF_UP);
        return new StoreOrderStats(total, byStatus, completionRate);
    }
}
