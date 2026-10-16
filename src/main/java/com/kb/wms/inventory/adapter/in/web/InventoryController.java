package com.kb.wms.inventory.adapter.in.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.common.response.PageResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inventory.adapter.in.web.dto.request.InventoryAdjustRequest;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryAdjustmentResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryDetailResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryLotViewResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventorySectionSummaryResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventorySkuSummaryResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryTransactionHistoryResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryTransactionResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.LowStockItemResponse;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySectionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 재고 조회/조정. GET /api/v1/inventory(/by-section|/by-lot|/{inventoryId}|/low-stock|/transactions|/{inventoryId}/transactions),
 * POST /api/v1/inventory/adjustments
 * 페이지네이션은 프로젝트 전체에 아직 도입하지 않아(#20 1단계) page/size/sort는 받지 않고 전체 목록을 반환한다.
 * 조정 처리자는 토큰 주체다. 담당 창고 범위 검사는 서비스가 한다.
 */
@Tag(name = "재고")
@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final InventoryAdjustmentUseCase inventoryAdjustmentUseCase;

    /** 목록 정렬 규칙. 기본은 SKU 코드순. */
    static final SortSpec SKU_SORT = SortSpec.builder()
            .field("skuCode", "s.skuCode")
            .field("skuName", "s.name")
            .field("totalQuantity", "sum(il.onHandQuantity)")
            .field("availableQuantity", "sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE then il.onHandQuantity - il.allocatedQuantity else 0L end)")
            .field("allocatedQuantity", "sum(il.allocatedQuantity)")
            .field("defectiveQuantity", "sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.DEFECTIVE then il.onHandQuantity else 0L end)")
            .defaultOrder("skuCode", SortDirection.ASC)
            .tieBreaker("s.skuId")
            .build();

    /** 구역별 정렬 규칙. 기본은 창고, 구역 코드, SKU 코드순. 같은 값은 구역+SKU로 순서를 고정한다. */
    static final SortSpec SECTION_SORT = SortSpec.builder()
            .field("warehouseId", "w.warehouseId")
            .field("warehouseName", "w.name")
            .field("sectionCode", "ws.sectionCode")
            .field("skuCode", "s.skuCode")
            .field("skuName", "s.name")
            .field("categoryName", "c.name")
            .field("totalQuantity", "sum(il.onHandQuantity)")
            .field("availableQuantity", "sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE then il.onHandQuantity - il.allocatedQuantity else 0L end)")
            .field("allocatedQuantity", "sum(il.allocatedQuantity)")
            .field("defectiveQuantity", "sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.DEFECTIVE then il.onHandQuantity else 0L end)")
            .defaultOrder("warehouseId", SortDirection.ASC)
            .defaultOrder("sectionCode", SortDirection.ASC)
            .defaultOrder("skuCode", SortDirection.ASC)
            .tieBreaker("ws.sectionId", "s.skuId")
            .build();

    /** 로트별 정렬 규칙. 기본은 유통기한 임박순(없으면 뒤로). */
    static final SortSpec LOT_VIEW_SORT = SortSpec.builder()
            .field("expiryDate", "coalesce(l.expiryDate, {d '9999-12-31'})")
            .field("lotNumber", "l.lotNumber")
            .field("skuCode", "s.skuCode")
            .field("sectionCode", "ws.sectionCode")
            .field("onHandQuantity", "il.onHandQuantity")
            .field("allocatedQuantity", "il.allocatedQuantity")
            .field("availableQuantity", "(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE then il.onHandQuantity - il.allocatedQuantity else 0L end)")
            .field("qualityStatus", "il.qualityStatus")
            .field("lastCountedAt", "il.lastCountedAt")
            .defaultOrder("expiryDate", SortDirection.ASC)
            .tieBreaker("il.inventoryLotId")
            .build();

    /** 재고 이력 정렬 규칙. 기본은 최신순. */
    static final SortSpec TRANSACTION_SORT = SortSpec.builder()
            .field("createdAt", "t.createdAt")
            .field("transactionType", "t.transactionType")
            .field("quantityDelta", "t.quantityDelta")
            .field("skuCode", "s.skuCode")
            .field("sectionCode", "ws.sectionCode")
            .field("lotNumber", "l.lotNumber")
            .field("referenceType", "t.referenceType")
            .defaultOrder("createdAt", SortDirection.DESC)
            .tieBreaker("t.transactionId")
            .build();

    @GetMapping("/api/v1/inventory")
    public ApiResponse<PageResponse<InventorySkuSummaryResponse>> getInventories(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, SKU_SORT);
        return ApiResponse.ok(PageResponse.of(
                inventoryQueryUseCase.getInventories(
                        InventorySearchCondition.unscoped(skuId, warehouseId, keyword), pageCondition, principal),
                InventorySkuSummaryResponse::from));
    }

    @GetMapping("/api/v1/inventory/by-section")
    public ApiResponse<PageResponse<InventorySectionSummaryResponse>> getInventoriesBySection(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, SECTION_SORT);
        return ApiResponse.ok(PageResponse.of(
                inventoryQueryUseCase.getInventoriesBySection(InventorySectionSearchCondition.unscoped(
                        warehouseId, sectionId, categoryId, skuId, keyword), pageCondition, principal),
                InventorySectionSummaryResponse::from));
    }

    @GetMapping("/api/v1/inventory/by-lot")
    public ApiResponse<PageResponse<InventoryLotViewResponse>> getInventoriesByLot(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @RequestParam(required = false) QualityStatus qualityStatus,
            @RequestParam(required = false) Boolean includeEmpty,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, LOT_VIEW_SORT);
        return ApiResponse.ok(PageResponse.of(
                inventoryQueryUseCase.searchInventoriesByLot(InventoryLotSearchCondition.unscoped(
                        skuId, warehouseId, sectionId, null, expiringBefore, qualityStatus, includeEmpty),
                        pageCondition, principal),
                InventoryLotViewResponse::from));
    }

    @GetMapping("/api/v1/inventory/{inventoryId}")
    public ApiResponse<InventoryDetailResponse> getInventory(@AuthenticationPrincipal AuthenticatedUser principal,
                                                            @PathVariable Long inventoryId) {
        InventoryDetail detail = inventoryQueryUseCase.getInventory(inventoryId, principal);
        return ApiResponse.ok(InventoryDetailResponse.from(detail));
    }

    @GetMapping("/api/v1/inventory/low-stock")
    public ApiResponse<ItemsResponse<LowStockItemResponse>> getLowStock(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword) {
        List<LowStockItemResponse> items = inventoryQueryUseCase
                .getLowStock(LowStockSearchCondition.unscoped(warehouseId, keyword), principal).stream()
                .map(LowStockItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/inventory/transactions")
    public ApiResponse<PageResponse<InventoryTransactionResponse>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, TRANSACTION_SORT);
        return ApiResponse.ok(PageResponse.of(
                inventoryQueryUseCase.getTransactions(
                        InventoryTransactionSearchCondition.unscoped(null, warehouseId, sectionId, skuId, lotId,
                                transactionType, referenceType, referenceId, createdFrom, createdTo),
                        pageCondition, principal),
                InventoryTransactionResponse::from));
    }

    @GetMapping("/api/v1/inventory/{inventoryId}/transactions")
    public ApiResponse<InventoryTransactionHistoryResponse> getTransactionsOf(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long inventoryId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<InventoryTransactionResponse> items = inventoryQueryUseCase.getTransactionsOf(inventoryId,
                        InventoryTransactionSearchCondition.unscoped(inventoryId, null, null, null, null,
                                transactionType, null, null, createdFrom, createdTo), principal)
                .stream()
                .map(InventoryTransactionResponse::from)
                .toList();
        return ApiResponse.ok(InventoryTransactionHistoryResponse.of(inventoryId, items));
    }

    @PostMapping("/api/v1/inventory/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryAdjustmentResponse> adjust(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody InventoryAdjustRequest request) {
        InventoryAdjustmentResponse response = InventoryAdjustmentResponse.from(
                inventoryAdjustmentUseCase.adjust(request.toCommand(principal.userId()), principal));
        return ApiResponse.created(response);
    }

}
