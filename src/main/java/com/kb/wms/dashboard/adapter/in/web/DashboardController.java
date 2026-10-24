package com.kb.wms.dashboard.adapter.in.web;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.dashboard.adapter.in.web.dto.response.HqDashboardSummaryResponse;
import com.kb.wms.dashboard.adapter.in.web.dto.response.HqDashboardTrendsResponse;
import com.kb.wms.dashboard.application.port.in.HqDashboardUseCase;
import com.kb.wms.dashboard.application.port.in.query.HqSummaryQuery;
import com.kb.wms.dashboard.application.port.in.query.HqTrendsQuery;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 본사 홈 대시보드. GET /api/v1/dashboard/hq/summary, GET /api/v1/dashboard/hq/trends
 * 본사 관리자 전용(SecurityConfig). 페이지네이션이 없어 page/size/sort는 받지 않고 무시한다.
 * month 형식 오류는 400, 숫자 범위 검사는 서비스가 한다.
 */
@Tag(name = "대시보드")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final HqDashboardUseCase hqDashboardUseCase;

    @GetMapping("/api/v1/dashboard/hq/summary")
    public ApiResponse<HqDashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Integer expiringWithinDays) {
        return ApiResponse.ok(HqDashboardSummaryResponse.from(
                hqDashboardUseCase.getSummary(new HqSummaryQuery(month, expiringWithinDays))));
    }

    @GetMapping("/api/v1/dashboard/hq/trends")
    public ApiResponse<HqDashboardTrendsResponse> getTrends(
            @RequestParam(required = false) Integer months,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.ok(HqDashboardTrendsResponse.from(
                hqDashboardUseCase.getTrends(new HqTrendsQuery(months, warehouseId))));
    }
}
