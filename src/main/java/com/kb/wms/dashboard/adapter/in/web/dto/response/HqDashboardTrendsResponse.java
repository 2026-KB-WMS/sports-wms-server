package com.kb.wms.dashboard.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.dashboard.application.port.in.result.HqDashboardTrends;
import com.kb.wms.dashboard.application.port.in.result.MonthlyFlow;
import com.kb.wms.dashboard.application.port.in.result.TopOutboundSku;

/**
 * GET /api/v1/dashboard/hq/trends 응답. 고정 개수 목록이라 페이지네이션 정보는 없다.
 * month는 {@code YYYY-MM} 문자열이고, 진행중 수량은 현재 달 항목에만 값이 있다.
 */
public record HqDashboardTrendsResponse(
        List<Item> items,
        List<TopSku> topOutboundSkus
) {

    public static HqDashboardTrendsResponse from(HqDashboardTrends trends) {
        return new HqDashboardTrendsResponse(
                trends.items().stream().map(Item::from).toList(),
                trends.topOutboundSkus().stream().map(TopSku::from).toList());
    }

    public record Item(
            String month,
            Long inboundCompletedQuantity,
            Long outboundCompletedQuantity,
            Long inboundInProgressQuantity,
            Long outboundInProgressQuantity
    ) {
        static Item from(MonthlyFlow flow) {
            return new Item(flow.month().toString(), flow.inboundCompletedQuantity(),
                    flow.outboundCompletedQuantity(), flow.inboundInProgressQuantity(),
                    flow.outboundInProgressQuantity());
        }
    }

    public record TopSku(
            Long skuId,
            String skuCode,
            String skuName,
            Long quantity
    ) {
        static TopSku from(TopOutboundSku sku) {
            return new TopSku(sku.skuId(), sku.skuCode(), sku.skuName(), sku.quantity());
        }
    }
}
