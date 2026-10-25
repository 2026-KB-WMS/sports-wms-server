package com.kb.wms.dashboard.adapter.in.web.warehouse.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.kb.wms.dashboard.application.port.in.warehouse.result.CapacityUsage;
import com.kb.wms.dashboard.application.port.in.warehouse.result.InboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.LowStockSku;
import com.kb.wms.dashboard.application.port.in.warehouse.result.OutboundWorkCounts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseDashboardSummary;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseInventoryAlerts;
import com.kb.wms.dashboard.application.port.in.warehouse.result.WarehouseWorkQueue;

/**
 * GET /api/v1/dashboard/warehouse/summary 응답.
 */
public record WarehouseDashboardSummaryResponse(
        Long warehouseId,
        String warehouseName,
        Pending pending,
        Capacity capacity,
        Inventory inventory
) {

    public static WarehouseDashboardSummaryResponse from(WarehouseDashboardSummary summary) {
        return new WarehouseDashboardSummaryResponse(
                summary.warehouseId(),
                summary.warehouseName(),
                Pending.from(summary.pending()),
                Capacity.from(summary.capacity()),
                Inventory.from(summary.inventory()));
    }

    public record Pending(
            Inbound inbound,
            Outbound outbound
    ) {
        static Pending from(WarehouseWorkQueue queue) {
            return new Pending(Inbound.from(queue.inbound()), Outbound.from(queue.outbound()));
        }
    }

    public record Inbound(
            Long arrivedCount,
            Long inspectingCount
    ) {
        static Inbound from(InboundWorkCounts counts) {
            return new Inbound(counts.arrivedCount(), counts.inspectingCount());
        }
    }

    public record Outbound(
            Long readyCount,
            Long pickingCount,
            Long shippedCount
    ) {
        static Outbound from(OutboundWorkCounts counts) {
            return new Outbound(counts.readyCount(), counts.pickingCount(), counts.shippedCount());
        }
    }

    public record Capacity(
            BigDecimal capacity,
            BigDecimal currentCapacity,
            BigDecimal availableCapacity,
            BigDecimal usageRate
    ) {
        static Capacity from(CapacityUsage usage) {
            return new Capacity(usage.capacity(), usage.currentCapacity(), usage.availableCapacity(),
                    usage.usageRate());
        }
    }

    public record Inventory(
            Long lowStockSkuCount,
            List<LowStockSkuItem> lowStockSkus,
            Long expiringLotCount
    ) {
        static Inventory from(WarehouseInventoryAlerts alerts) {
            return new Inventory(alerts.lowStockSkuCount(),
                    alerts.lowStockSkus().stream().map(LowStockSkuItem::from).toList(),
                    alerts.expiringLotCount());
        }
    }

    public record LowStockSkuItem(
            Long skuId,
            String skuCode,
            String skuName,
            Long availableQuantity,
            Long safetyStockQuantity
    ) {
        static LowStockSkuItem from(LowStockSku sku) {
            return new LowStockSkuItem(sku.skuId(), sku.skuCode(), sku.skuName(), sku.availableQuantity(),
                    sku.safetyStockQuantity());
        }
    }
}
