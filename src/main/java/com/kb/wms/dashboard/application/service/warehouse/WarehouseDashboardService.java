package com.kb.wms.dashboard.application.service.warehouse;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.dashboard.application.port.in.warehouse.WarehouseDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.warehouse.query.WarehouseSummaryQuery;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseDashboardSummary;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseWorkQueue;
import com.kb.wms.dashboard.application.port.out.warehouse.WarehouseDashboardQueryRepository;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.result.WarehouseView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseDashboardService implements WarehouseDashboardUseCase {

    static final int DEFAULT_EXPIRING_WITHIN_DAYS = 30;
    static final int MAX_EXPIRING_WITHIN_DAYS = 365;
    static final int LOW_STOCK_SKU_LIMIT = 5;

    private final WarehouseDashboardQueryRepository dashboardQueryRepository;
    private final WarehouseUseCase warehouseUseCase;

    @Override
    public WarehouseDashboardSummary getSummary(WarehouseSummaryQuery query, AuthenticatedUser principal) {
        Long warehouseId = resolveWarehouseId(query.warehouseId(), principal);
        int expiringWithinDays = query.expiringWithinDays() != null
                ? query.expiringWithinDays() : DEFAULT_EXPIRING_WITHIN_DAYS;
        if (expiringWithinDays < 1 || expiringWithinDays > MAX_EXPIRING_WITHIN_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "expiringWithinDays는 1 이상 " + MAX_EXPIRING_WITHIN_DAYS + " 이하여야 합니다.");
        }

        WarehouseView warehouse = warehouseUseCase.getWarehouseView(warehouseId);
        return new WarehouseDashboardSummary(
                warehouseId,
                warehouse.name(),
                new WarehouseWorkQueue(
                        dashboardQueryRepository.findInboundWorkCounts(warehouseId),
                        dashboardQueryRepository.findOutboundWorkCounts(warehouseId)),
                dashboardQueryRepository.findCapacityUsage(warehouseId),
                dashboardQueryRepository.findInventoryAlerts(warehouseId,
                        LocalDate.now().plusDays(expiringWithinDays), LOW_STOCK_SKU_LIMIT));
    }

    /**
     * 조회할 창고. 지정했으면 담당 여부만 확인하고(아니면 존재 여부와 관계없이 403),
     * 생략했으면 담당 창고가 하나일 때만 그 창고를 쓴다. 여럿이면 400, 담당 창고가 없으면 403.
     */
    private Long resolveWarehouseId(Long requestedWarehouseId, AuthenticatedUser principal) {
        if (requestedWarehouseId != null) {
            principal.requireWarehouseAccess(requestedWarehouseId);
            return requestedWarehouseId;
        }
        List<Long> warehouseIds = principal.warehouseIds();
        if (warehouseIds.isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (warehouseIds.size() > 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "담당 창고가 여럿이면 warehouseId가 필요합니다.");
        }
        return warehouseIds.get(0);
    }
}
