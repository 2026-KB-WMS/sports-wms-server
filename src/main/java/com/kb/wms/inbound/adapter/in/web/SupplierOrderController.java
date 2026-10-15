package com.kb.wms.inbound.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.adapter.in.web.dto.response.SupplierOrderResponse;
import com.kb.wms.inbound.adapter.in.web.dto.response.SupplierOrderSummaryResponse;
import com.kb.wms.inbound.application.port.in.SupplierOrderUseCase;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 공급처 발주서 조회. GET /api/v1/supplier-orders, GET .../{supplierOrderId}
 * 발주서는 발주 확정으로만 만들어지므로 쓰기 API가 없다. 목록은 페이지네이션 없이 전체를 반환한다.
 */
@Tag(name = "공급처 발주서")
@RestController
@RequestMapping("/api/v1/supplier-orders")
@RequiredArgsConstructor
public class SupplierOrderController {

    private final SupplierOrderUseCase supplierOrderUseCase;

    @GetMapping
    public ApiResponse<ItemsResponse<SupplierOrderSummaryResponse>> getSupplierOrders(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) SupplierOrderStatus status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long purchaseOrderId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        List<SupplierOrderSummaryResponse> items = supplierOrderUseCase
                .getSupplierOrders(SupplierOrderSearchCondition.unscoped(
                        status, warehouseId, supplierId, purchaseOrderId, keyword, createdFrom, createdTo), principal)
                .stream()
                .map(SupplierOrderSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{supplierOrderId}")
    public ApiResponse<SupplierOrderResponse> getSupplierOrder(@AuthenticationPrincipal AuthenticatedUser principal,
                                                               @PathVariable Long supplierOrderId) {
        return ApiResponse.ok(SupplierOrderResponse.from(
                supplierOrderUseCase.getSupplierOrder(supplierOrderId, principal)));
    }
}
