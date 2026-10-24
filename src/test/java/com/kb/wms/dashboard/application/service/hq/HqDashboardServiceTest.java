package com.kb.wms.dashboard.application.service.hq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.dashboard.application.port.in.hq.query.HqSummaryQuery;
import com.kb.wms.dashboard.application.port.in.hq.query.HqTrendsQuery;
import com.kb.wms.dashboard.application.port.in.hq.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardSummary;
import com.kb.wms.dashboard.application.port.in.hq.result.HqDashboardTrends;
import com.kb.wms.dashboard.application.port.in.hq.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.hq.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.hq.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.hq.result.TopOutboundSku;
import com.kb.wms.dashboard.application.port.out.hq.HqDashboardQueryRepository;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class HqDashboardServiceTest {

    @Mock private HqDashboardQueryRepository repository;
    @Mock private WarehouseUseCase warehouseUseCase;
    @InjectMocks private HqDashboardService service;

    private static final YearMonth OCT = YearMonth.of(2026, 10);

    @Test
    @DisplayName("요약은 기본값(현재 달, 임박 30일)을 채우고 전월 완료 비교값을 함께 조회한다")
    void summaryDefaults() {
        when(repository.findInventoryTotals(any())).thenReturn(new InventoryTotals(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        when(repository.findInboundCompleted(any(), eq(null))).thenReturn(new FlowCount(1L, 10L));
        when(repository.findInboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        when(repository.findOutboundCompleted(any(), eq(null))).thenReturn(new FlowCount(2L, 20L));
        when(repository.findOutboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        when(repository.countStoreOrdersByStatus(any())).thenReturn(Map.of());
        when(repository.findPendingCounts()).thenReturn(new PendingCounts(1L, 2L, 3L));

        HqDashboardSummary summary = service.getSummary(new HqSummaryQuery(null, null));

        YearMonth current = YearMonth.now();
        assertThat(summary.month()).isEqualTo(current);
        assertThat(summary.pending().unassignedApprovedStoreOrderCount()).isEqualTo(3L);
        verify(repository).findInventoryTotals(LocalDate.now().plusDays(30));
        verify(repository).findInboundCompleted(current, null);
        verify(repository).findInboundCompleted(current.minusMonths(1), null);
        verify(repository).findOutboundCompleted(current.minusMonths(1), null);
    }

    @Test
    @DisplayName("지점 발주 상태는 0건도 모두 채우고 완료율은 취소·반려를 분모에서 뺀다")
    void storeOrderStatsFillsZeroAndCompletionRate() {
        stubEmptySummary();
        when(repository.countStoreOrdersByStatus(OCT)).thenReturn(
                Map.of("COMPLETED", 6L, "ASSIGNED", 1L, "CANCELED", 2L, "REJECTED", 1L));

        var stats = service.getSummary(new HqSummaryQuery(OCT, 30)).storeOrders();

        assertThat(stats.totalCount()).isEqualTo(10L);
        assertThat(stats.byStatus()).containsExactly(
                Map.entry("REQUESTED", 0L), Map.entry("APPROVED", 0L), Map.entry("ASSIGNED", 1L),
                Map.entry("ON_HOLD", 0L), Map.entry("COMPLETED", 6L), Map.entry("CANCELED", 2L),
                Map.entry("REJECTED", 1L));
        // 6 / (10 - 2 - 1) = 0.857...
        assertThat(stats.completionRate()).isEqualByComparingTo(new BigDecimal("0.86"));
    }

    @Test
    @DisplayName("완료율의 분모가 0이면 null이다")
    void completionRateIsNullWhenDenominatorZero() {
        stubEmptySummary();
        when(repository.countStoreOrdersByStatus(OCT)).thenReturn(Map.of("CANCELED", 2L, "REJECTED", 1L));

        var stats = service.getSummary(new HqSummaryQuery(OCT, 30)).storeOrders();
        assertThat(stats.completionRate()).isNull();

        when(repository.countStoreOrdersByStatus(OCT)).thenReturn(Map.of());
        stats = service.getSummary(new HqSummaryQuery(OCT, 30)).storeOrders();
        assertThat(stats.totalCount()).isZero();
        assertThat(stats.completionRate()).isNull();
    }

    @Test
    @DisplayName("임박 기준 일수가 1~365 밖이면 400이다")
    void expiringWithinDaysOutOfRange() {
        for (int days : new int[] {0, -1, 366}) {
            assertThatThrownBy(() -> service.getSummary(new HqSummaryQuery(OCT, days)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("추이는 데이터 없는 달을 0으로 채우고 진행중 수량은 현재 달에만 담는다")
    void trendsFillsMissingMonthsAndInProgressOnlyForCurrent() {
        YearMonth current = YearMonth.now();
        when(repository.findMonthlyCompleted(current.minusMonths(2), current, null)).thenReturn(List.of(
                new MonthlyFlow(current.minusMonths(2), 100L, 40L, null, null)));
        when(repository.findInboundInProgress(null)).thenReturn(new FlowCount(1L, 7L));
        when(repository.findOutboundInProgress(null)).thenReturn(new FlowCount(2L, 9L));
        when(repository.findTopOutboundSkus(current, null, 5)).thenReturn(List.of());

        HqDashboardTrends trends = service.getTrends(new HqTrendsQuery(3, null));

        assertThat(trends.items()).containsExactly(
                new MonthlyFlow(current.minusMonths(2), 100L, 40L, null, null),
                new MonthlyFlow(current.minusMonths(1), 0L, 0L, null, null),
                new MonthlyFlow(current, 0L, 0L, 7L, 9L));
        verifyNoInteractions(warehouseUseCase);
    }

    @Test
    @DisplayName("추이는 기본 6개월이고 출고 상위 SKU는 5개까지 조회한다")
    void trendsDefaultsToSixMonths() {
        YearMonth current = YearMonth.now();
        when(repository.findMonthlyCompleted(current.minusMonths(5), current, null)).thenReturn(List.of());
        when(repository.findInboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        when(repository.findOutboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        List<TopOutboundSku> top = List.of(new TopOutboundSku(1L, "SKU-1", "축구공", 50L));
        when(repository.findTopOutboundSkus(current, null, 5)).thenReturn(top);

        HqDashboardTrends trends = service.getTrends(new HqTrendsQuery(null, null));

        assertThat(trends.items()).hasSize(6);
        assertThat(trends.items().get(0).month()).isEqualTo(current.minusMonths(5));
        assertThat(trends.topOutboundSkus()).isEqualTo(top);
    }

    @Test
    @DisplayName("추이의 달 수가 1~12 밖이면 400이다")
    void trendsMonthsOutOfRange() {
        for (int months : new int[] {0, 13}) {
            assertThatThrownBy(() -> service.getTrends(new HqTrendsQuery(months, null)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("없는 창고로 추이를 조회하면 WAREHOUSE_NOT_FOUND이고 집계는 하지 않는다")
    void trendsWarehouseNotFound() {
        when(warehouseUseCase.getWarehouseView(99L))
                .thenThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        assertThatThrownBy(() -> service.getTrends(new HqTrendsQuery(6, 99L)))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).findMonthlyCompleted(any(), any(), any());
    }

    private void stubEmptySummary() {
        when(repository.findPendingCounts()).thenReturn(new PendingCounts(0L, 0L, 0L));
        when(repository.findInventoryTotals(any())).thenReturn(new InventoryTotals(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        when(repository.findInboundCompleted(any(), eq(null))).thenReturn(new FlowCount(0L, 0L));
        when(repository.findInboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        when(repository.findOutboundCompleted(any(), eq(null))).thenReturn(new FlowCount(0L, 0L));
        when(repository.findOutboundInProgress(null)).thenReturn(new FlowCount(0L, 0L));
        when(repository.findWarehouseRows(any())).thenReturn(List.of());
    }
}
