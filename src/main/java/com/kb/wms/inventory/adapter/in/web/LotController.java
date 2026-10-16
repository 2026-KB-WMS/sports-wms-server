package com.kb.wms.inventory.adapter.in.web;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.common.response.PageResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inventory.adapter.in.web.dto.request.LotStatusChangeRequest;
import com.kb.wms.inventory.adapter.in.web.dto.response.LotDetailResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.LotSummaryResponse;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 로트 마스터 조회·상태 변경. GET /api/v1/lots, /lots/{lotId}, PATCH /lots/{lotId}/status
 * 로트는 별도 생성 API 없이 입고 검수 트랜잭션 안에서 만들어진다(ADR-004).
 */
@Tag(name = "로트")
@RestController
@RequiredArgsConstructor
public class LotController {

    private final LotUseCase lotUseCase;
    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final WarehouseUseCase warehouseUseCase;

    /** 목록 정렬 규칙. 기본은 유통기한 임박순(없으면 뒤로). */
    static final SortSpec LIST_SORT = SortSpec.builder()
            .field("expiryDate", "coalesce(l.expiryDate, {d '9999-12-31'})")
            .field("lotNumber", "l.lotNumber")
            .field("skuCode", "s.skuCode")
            .field("supplierName", "su.name")
            .field("status", "l.status")
            .field("createdAt", "l.createdAt")
            .field("unitCost", "l.unitCost")
            .defaultOrder("expiryDate", SortDirection.ASC)
            .tieBreaker("l.lotId")
            .build();

    @GetMapping("/api/v1/lots")
    public ApiResponse<PageResponse<LotSummaryResponse>> getLots(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, LIST_SORT);
        return ApiResponse.ok(PageResponse.of(
                lotUseCase.getLots(LotSearchCondition.unscoped(skuId, supplierId, expiringBefore, keyword),
                        pageCondition, principal),
                LotSummaryResponse::from));
    }

    @GetMapping("/api/v1/lots/{lotId}")
    public ApiResponse<LotDetailResponse> getLot(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @PathVariable Long lotId) {
        LotSummary summary = lotUseCase.getLot(lotId, principal);
        // 같은 창고를 여러 행이 공유하므로 창고명은 창고당 한 번만 조회한다.
        Map<Long, String> warehouseNames = new HashMap<>();
        List<LotDetailResponse.InventoryItem> inventory = inventoryQueryUseCase
                .getInventoriesByLot(InventoryLotSearchCondition.ofLot(lotId), principal).stream()
                .map(view -> toInventoryItem(view, warehouseNames))
                .toList();
        List<LotDetailResponse.InboundItem> inbounds = lotUseCase.getLotInbounds(lotId, principal).stream()
                .map(LotDetailResponse.InboundItem::from)
                .toList();
        return ApiResponse.ok(LotDetailResponse.of(summary, inventory, inbounds));
    }

    @PatchMapping("/api/v1/lots/{lotId}/status")
    public ApiResponse<LotSummaryResponse> changeLotStatus(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @PathVariable Long lotId,
                                                           @Valid @RequestBody LotStatusChangeRequest request) {
        lotUseCase.changeLotStatus(lotId, request.toCommand(principal.userId()));
        return ApiResponse.ok(LotSummaryResponse.from(lotUseCase.getLot(lotId, principal)));
    }

    private LotDetailResponse.InventoryItem toInventoryItem(InventoryLotView view, Map<Long, String> warehouseNames) {
        String warehouseName = warehouseNames.computeIfAbsent(view.warehouseId(),
                id -> warehouseUseCase.getWarehouseView(id).name());
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
