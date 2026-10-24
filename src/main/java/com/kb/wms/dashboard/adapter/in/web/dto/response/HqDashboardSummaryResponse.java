package com.kb.wms.dashboard.adapter.in.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.kb.wms.dashboard.application.port.in.result.FlowCount;
import com.kb.wms.dashboard.application.port.in.result.FlowTotals;
import com.kb.wms.dashboard.application.port.in.result.HqDashboardSummary;
import com.kb.wms.dashboard.application.port.in.result.InventoryTotals;
import com.kb.wms.dashboard.application.port.in.result.PendingCounts;
import com.kb.wms.dashboard.application.port.in.result.StoreOrderStats;
import com.kb.wms.dashboard.application.port.in.result.WarehouseDashboardRow;
import com.kb.wms.dashboard.application.port.in.result.WarehouseFlowQuantity;

/**
 * GET /api/v1/dashboard/hq/summary 응답. month는 {@code YYYY-MM} 문자열이다.
 */
public record HqDashboardSummaryResponse(
        String month,
        Pending pending,
        Inventory inventory,
        Flow inbound,
        Flow outbound,
        StoreOrders storeOrders,
        List<WarehouseRow> warehouses
) {

    public static HqDashboardSummaryResponse from(HqDashboardSummary summary) {
        return new HqDashboardSummaryResponse(
                summary.month().toString(),
                Pending.from(summary.pending()),
                Inventory.from(summary.inventory()),
                Flow.from(summary.inbound()),
                Flow.from(summary.outbound()),
                StoreOrders.from(summary.storeOrders()),
                summary.warehouses().stream().map(WarehouseRow::from).toList());
    }

    public record Pending(
            Long storeOrderRequestedCount,
            Long purchaseOrderRequestedCount,
            Long unassignedApprovedStoreOrderCount
    ) {
        static Pending from(PendingCounts pending) {
            return new Pending(pending.storeOrderRequestedCount(), pending.purchaseOrderRequestedCount(),
                    pending.unassignedApprovedStoreOrderCount());
        }
    }

    public record Inventory(
            Long totalQuantity,
            Long quarantinedQuantity,
            Long disposedQuantity,
            Long expiredQuantity,
            Long defectiveQuantity,
            Long activeSkuCount,
            Long lowStockSkuCount,
            Long expiringLotCount
    ) {
        static Inventory from(InventoryTotals inventory) {
            return new Inventory(inventory.totalQuantity(), inventory.quarantinedQuantity(),
                    inventory.disposedQuantity(), inventory.expiredQuantity(), inventory.defectiveQuantity(),
                    inventory.activeSkuCount(), inventory.lowStockSkuCount(), inventory.expiringLotCount());
        }
    }

    public record Flow(
            Count completed,
            Count inProgress,
            Count previousMonthCompleted
    ) {
        static Flow from(FlowTotals totals) {
            return new Flow(Count.from(totals.completed()), Count.from(totals.inProgress()),
                    Count.from(totals.previousMonthCompleted()));
        }
    }

    public record Count(
            Long count,
            Long quantity
    ) {
        static Count from(FlowCount flow) {
            return new Count(flow.count(), flow.quantity());
        }
    }

    public record StoreOrders(
            Long totalCount,
            Map<String, Long> byStatus,
            BigDecimal completionRate
    ) {
        static StoreOrders from(StoreOrderStats stats) {
            return new StoreOrders(stats.totalCount(), stats.byStatus(), stats.completionRate());
        }
    }

    public record WarehouseRow(
            Long warehouseId,
            String warehouseName,
            Long inventoryQuantity,
            Long lowStockSkuCount,
            Quantity inbound,
            Quantity outbound
    ) {
        static WarehouseRow from(WarehouseDashboardRow row) {
            return new WarehouseRow(row.warehouseId(), row.warehouseName(), row.inventoryQuantity(),
                    row.lowStockSkuCount(), Quantity.from(row.inbound()), Quantity.from(row.outbound()));
        }
    }

    public record Quantity(
            Long completedQuantity,
            Long inProgressQuantity
    ) {
        static Quantity from(WarehouseFlowQuantity quantity) {
            return new Quantity(quantity.completedQuantity(), quantity.inProgressQuantity());
        }
    }
}
