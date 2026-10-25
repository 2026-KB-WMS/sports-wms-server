package com.kb.wms.dashboard.application.service.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.dashboard.application.port.in.warehouse.query.WarehouseSummaryQuery;
import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseDashboardSummary;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;
import com.kb.wms.dashboard.application.port.out.warehouse.WarehouseDashboardQueryRepository;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.result.WarehouseView;

@ExtendWith(MockitoExtension.class)
class WarehouseDashboardServiceTest {

    @Mock private WarehouseDashboardQueryRepository repository;
    @Mock private WarehouseUseCase warehouseUseCase;
    @InjectMocks private WarehouseDashboardService service;

    private static AuthenticatedUser manager(Long... warehouseIds) {
        return new AuthenticatedUser(10L, UserRole.WAREHOUSE_MANAGER, List.of(warehouseIds), List.of());
    }

    private void stubAggregates(Long warehouseId, LocalDate expiringOnOrBefore) {
        when(warehouseUseCase.getWarehouseView(warehouseId)).thenReturn(new WarehouseView(warehouseId, "서울 센터", true));
        when(repository.findInboundWorkCounts(warehouseId)).thenReturn(new InboundWorkCounts(2L, 1L));
        when(repository.findOutboundWorkCounts(warehouseId)).thenReturn(new OutboundWorkCounts(5L, 2L, 3L));
        when(repository.findCapacityUsage(warehouseId))
                .thenReturn(CapacityUsage.of(BigDecimal.valueOf(10000), BigDecimal.valueOf(6200)));
        when(repository.findInventoryAlerts(warehouseId, expiringOnOrBefore, 5))
                .thenReturn(new WarehouseInventoryAlerts(0L, List.of(), 0L));
    }

    @Test
    @DisplayName("담당 창고가 하나면 warehouseId 없이 그 창고를 기본 임박 30일로 조회한다")
    void singleWarehouseDefaults() {
        stubAggregates(1L, LocalDate.now().plusDays(30));

        WarehouseDashboardSummary summary = service.getSummary(new WarehouseSummaryQuery(null, null), manager(1L));

        assertThat(summary.warehouseId()).isEqualTo(1L);
        assertThat(summary.warehouseName()).isEqualTo("서울 센터");
        assertThat(summary.pending().inbound().arrivedCount()).isEqualTo(2L);
        assertThat(summary.pending().outbound().shippedCount()).isEqualTo(3L);
        assertThat(summary.capacity().usageRate()).isEqualByComparingTo("0.62");
        verify(repository).findInventoryAlerts(1L, LocalDate.now().plusDays(30), 5);
    }

    @Test
    @DisplayName("담당 창고가 여럿이면 지정한 창고와 임박 일수로 조회한다")
    void multipleWarehousesWithId() {
        stubAggregates(2L, LocalDate.now().plusDays(7));

        WarehouseDashboardSummary summary = service.getSummary(new WarehouseSummaryQuery(2L, 7), manager(1L, 2L));

        assertThat(summary.warehouseId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("담당 창고가 여럿인데 warehouseId가 없으면 400 VALIDATION_ERROR")
    void multipleWarehousesWithoutId() {
        assertThatThrownBy(() -> service.getSummary(new WarehouseSummaryQuery(null, null), manager(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(repository, warehouseUseCase);
    }

    @Test
    @DisplayName("담당이 아닌 창고는 대상을 조회하기 전에 403 FORBIDDEN")
    void notAssignedWarehouse() {
        assertThatThrownBy(() -> service.getSummary(new WarehouseSummaryQuery(99L, null), manager(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(repository, warehouseUseCase);
    }

    @Test
    @DisplayName("담당 창고가 없는데 warehouseId도 없으면 403 FORBIDDEN")
    void noAssignedWarehouse() {
        assertThatThrownBy(() -> service.getSummary(new WarehouseSummaryQuery(null, null), manager()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(repository, warehouseUseCase);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 366})
    @DisplayName("expiringWithinDays가 1~365를 벗어나면 400 VALIDATION_ERROR")
    void expiringWithinDaysOutOfRange(int days) {
        assertThatThrownBy(() -> service.getSummary(new WarehouseSummaryQuery(1L, days), manager(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(repository, warehouseUseCase);
    }

    @Test
    @DisplayName("사용률은 소수 둘째 자리 반올림, 가용량은 수용량 - 적치량, 수용량 0이면 사용률 null")
    void capacityUsage() {
        CapacityUsage usage = CapacityUsage.of(BigDecimal.valueOf(3), BigDecimal.valueOf(2));
        assertThat(usage.usageRate()).isEqualByComparingTo("0.67");
        assertThat(usage.availableCapacity()).isEqualByComparingTo("1");

        CapacityUsage empty = CapacityUsage.of(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(empty.usageRate()).isNull();
        assertThat(empty.availableCapacity()).isEqualByComparingTo("0");
    }
}
