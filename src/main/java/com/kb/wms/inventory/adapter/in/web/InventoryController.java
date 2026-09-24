package com.kb.wms.inventory.adapter.in.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.inventory.adapter.in.web.dto.request.InventoryAdjustRequest;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryAdjustmentResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryDetailResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryLotViewResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventorySkuSummaryResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryTransactionHistoryResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.InventoryTransactionResponse;
import com.kb.wms.inventory.adapter.in.web.dto.response.LowStockItemResponse;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 재고 조회/조정. GET /api/v1/inventory(/by-lot|/{inventoryId}|/low-stock|/transactions|/{inventoryId}/transactions),
 * POST /api/v1/inventory/adjustments
 * 페이지네이션은 프로젝트 전체에 아직 도입하지 않아(#20 1단계) page/size/sort는 받지 않고 전체 목록을 반환한다.
 * 인증이 없어 조정 처리자(userId)는 쿼리 파라미터로 받는다(창고 도메인 GET /warehouses/my와 동일한 임시 방식).
 */
@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final InventoryAdjustmentUseCase inventoryAdjustmentUseCase;

    @GetMapping("/api/v1/inventory")
    public ApiResponse<ItemsResponse<InventorySkuSummaryResponse>> getInventories(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword) {
        List<InventorySkuSummaryResponse> items = inventoryQueryUseCase
                .getInventories(new InventorySearchCondition(skuId, warehouseId, keyword)).stream()
                .map(InventorySkuSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/inventory/by-lot")
    public ApiResponse<ItemsResponse<InventoryLotViewResponse>> getInventoriesByLot(
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiringBefore,
            @RequestParam(required = false) QualityStatus qualityStatus,
            @RequestParam(required = false) Boolean includeEmpty) {
        List<InventoryLotViewResponse> items = inventoryQueryUseCase
                .getInventoriesByLot(new InventoryLotSearchCondition(
                        skuId, warehouseId, sectionId, null, expiringBefore, qualityStatus, includeEmpty))
                .stream()
                .map(InventoryLotViewResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/inventory/{inventoryId}")
    public ApiResponse<InventoryDetailResponse> getInventory(@PathVariable Long inventoryId) {
        InventoryDetailResponse response = InventoryDetailResponse.from(
                inventoryQueryUseCase.getInventory(inventoryId));
        return ApiResponse.ok(response);
    }

    @GetMapping("/api/v1/inventory/low-stock")
    public ApiResponse<ItemsResponse<LowStockItemResponse>> getLowStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword) {
        List<LowStockItemResponse> items = inventoryQueryUseCase
                .getLowStock(new LowStockSearchCondition(warehouseId, keyword)).stream()
                .map(LowStockItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/inventory/transactions")
    public ApiResponse<ItemsResponse<InventoryTransactionResponse>> getTransactions(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long skuId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<InventoryTransactionResponse> items = inventoryQueryUseCase.getTransactions(
                        new InventoryTransactionSearchCondition(null, warehouseId, sectionId, skuId, lotId,
                                transactionType, referenceType, referenceId, createdFrom, createdTo))
                .stream()
                .map(InventoryTransactionResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/api/v1/inventory/{inventoryId}/transactions")
    public ApiResponse<InventoryTransactionHistoryResponse> getTransactionsOf(
            @PathVariable Long inventoryId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<InventoryTransactionResponse> items = inventoryQueryUseCase.getTransactionsOf(inventoryId,
                        new InventoryTransactionSearchCondition(inventoryId, null, null, null, null,
                                transactionType, null, null, createdFrom, createdTo))
                .stream()
                .map(InventoryTransactionResponse::from)
                .toList();
        return ApiResponse.ok(InventoryTransactionHistoryResponse.of(inventoryId, items));
    }

    @PostMapping("/api/v1/inventory/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryAdjustmentResponse> adjust(
            @Valid @RequestBody InventoryAdjustRequest request,
            @RequestParam Long userId) {
        InventoryAdjustmentResponse response = InventoryAdjustmentResponse.from(
                inventoryAdjustmentUseCase.adjust(request.toCommand(userId)));
        return ApiResponse.created(response);
    }
}
