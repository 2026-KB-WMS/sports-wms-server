package com.kb.wms.dashboard.adapter.in.web.warehouse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.dashboard.adapter.in.web.warehouse.dto.response.WarehouseDashboardSummaryResponse;
import com.kb.wms.dashboard.application.port.in.warehouse.WarehouseDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.warehouse.query.WarehouseSummaryQuery;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 창고 관리자 홈 대시보드. GET /api/v1/dashboard/warehouse/summary
 * 창고 관리자 전용(SecurityConfig). 페이지네이션이 없어 page/size/sort는 받지 않고 무시한다.
 * 담당 창고 해석과 숫자 범위 검사는 서비스가 한다.
 */
@Tag(name = "대시보드")
@RestController
@RequiredArgsConstructor
public class WarehouseDashboardController {

    private final WarehouseDashboardUseCase warehouseDashboardUseCase;

    @GetMapping("/api/v1/dashboard/warehouse/summary")
    public ApiResponse<WarehouseDashboardSummaryResponse> getSummary(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer expiringWithinDays,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(WarehouseDashboardSummaryResponse.from(warehouseDashboardUseCase.getSummary(
                new WarehouseSummaryQuery(warehouseId, expiringWithinDays), principal)));
    }
}
