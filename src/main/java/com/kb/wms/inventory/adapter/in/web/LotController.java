package com.kb.wms.inventory.adapter.in.web;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.LotDetailResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.LotSummaryResponse;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 로트 마스터 조회. GET /api/v1/lots, /lots/{lotId}
 * 로트는 별도 생성 API 없이 입고 검수 트랜잭션 안에서 만들어진다(ADR-004).
 */
@RestController
@RequiredArgsConstructor
public class LotController {

    private final LotUseCase lotUseCase;
    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final WarehouseUseCase warehouseUseCase;

    @GetMapping("/api/v1/lots")
    public ApiResponse<ItemsResponse<LotSummaryResponse>> getLots(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @RequestParam(required = false) String keyword) {
        List<LotSummaryResponse> items = lotUseCase
                .getLots(new LotSearchCondition(skuId, supplierId, expiringBefore, keyword)).stream()
                .map(LotSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/lots/{lotId}")
    public ApiResponse<LotDetailResponse> getLot(@PathVariable Long lotId) {
        LotSummary summary = lotUseCase.getLot(lotId);
        // 같은 창고를 여러 행이 공유하므로 창고명은 창고당 한 번만 조회한다.
        Map<Long, String> warehouseNames = new HashMap<>();
        List<LotDetailResponse.InventoryItem> inventory = inventoryQueryUseCase
                .getInventoriesByLot(InventoryLotSearchCondition.ofLot(lotId)).stream()
                .map(view -> toInventoryItem(view, warehouseNames))
                .toList();
        return ApiResponse.ok(LotDetailResponse.of(summary, inventory));
    }

    private LotDetailResponse.InventoryItem toInventoryItem(InventoryLotView view, Map<Long, String> warehouseNames) {
        String warehouseName = warehouseNames.computeIfAbsent(view.warehouseId(),
                id -> warehouseUseCase.getWarehouse(id).getName());
        return new LotDetailResponse.InventoryItem(
                view.inventoryLotId(),
                view.warehouseId(),
                warehouseName,
                view.sectionId(),
                view.sectionCode(),
                view.sectionName(),
                view.onHandQuantity(),
                view.allocatedQuantity(),
                view.qualityStatus());
    }
}
