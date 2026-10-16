package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.paging.TestPages;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderDetails;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.inbound.exception.SupplierOrderErrorCode;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

@ExtendWith(MockitoExtension.class)
class SupplierOrderServiceTest {

    private static final PageCondition PAGE = TestPages.firstPage(
            TestPages.order("id", SortDirection.DESC));

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());
    private static final AuthenticatedUser MANAGER =
            new AuthenticatedUser(5L, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of());

    @Mock
    private SupplierOrderQueryRepository supplierOrderQueryRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private WarehouseAvailabilityPort warehouseAvailabilityPort;

    @InjectMocks
    private SupplierOrderService supplierOrderService;

    private static SupplierOrderView view(Long warehouseId) {
        return new SupplierOrderView(21L, "SO-20261015-0001", 4L, "PO-20261015-0001", 3L, "공급처 A",
                warehouseId, "서울 물류센터", SupplierOrderStatus.ISSUED, 1L, BigDecimal.valueOf(100000),
                LocalDateTime.of(2026, 10, 15, 9, 0), LocalDateTime.of(2026, 10, 15, 9, 0));
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    @Test
    @DisplayName("목록은 조건을 그대로 조회 포트에 위임하고 본사에는 범위 제한이 없다")
    void getSupplierOrders_delegates() {
        SupplierOrderSearchCondition condition = SupplierOrderSearchCondition.unscoped(
                SupplierOrderStatus.ISSUED, 1L, 3L, 4L, "SO", null, null);
        when(supplierRepository.existsById(3L)).thenReturn(true);
        when(purchaseOrderRepository.findById(4L)).thenReturn(Optional.of(
                com.kb.wms.inbound.domain.entity.PurchaseOrder.builder().purchaseOrderId(4L).build()));
        when(supplierOrderQueryRepository.search(condition, PAGE)).thenReturn(TestPages.resultOf(List.of()));

        assertThat(supplierOrderService.getSupplierOrders(condition, PAGE, HQ).items()).isEmpty();
        verify(warehouseAvailabilityPort).requireExists(1L);
    }

    @Test
    @DisplayName("창고 관리자는 창고를 생략하면 담당 창고로 좁히고, 비담당 창고를 지정하면 403이다")
    void getSupplierOrders_scope() {
        when(supplierOrderQueryRepository.search(
                new SupplierOrderSearchCondition(null, null, null, null, null, null, null, List.of(1L, 2L)), PAGE))
                .thenReturn(TestPages.resultOf(List.of()));

        assertThat(supplierOrderService.getSupplierOrders(
                SupplierOrderSearchCondition.unscoped(null, null, null, null, null, null, null), PAGE, MANAGER).items()).isEmpty();
        assertError(() -> supplierOrderService.getSupplierOrders(
                        SupplierOrderSearchCondition.unscoped(null, 3L, null, null, null, null, null), PAGE, MANAGER),
                ErrorCode.FORBIDDEN.name());
        verifyNoInteractions(warehouseAvailabilityPort);
    }

    @Test
    @DisplayName("없는 창고·공급처·발주 필터는 각 도메인의 404를 던지고 조회하지 않는다")
    void getSupplierOrders_unknownFilters() {
        doThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND))
                .when(warehouseAvailabilityPort).requireExists(999L);
        when(supplierRepository.existsById(999L)).thenReturn(false);
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        assertError(() -> supplierOrderService.getSupplierOrders(
                        SupplierOrderSearchCondition.unscoped(null, 999L, null, null, null, null, null), PAGE, HQ),
                WarehouseErrorCode.WAREHOUSE_NOT_FOUND.name());
        assertError(() -> supplierOrderService.getSupplierOrders(
                        SupplierOrderSearchCondition.unscoped(null, null, 999L, null, null, null, null), PAGE, HQ),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        assertError(() -> supplierOrderService.getSupplierOrders(
                        SupplierOrderSearchCondition.unscoped(null, null, null, 999L, null, null, null), PAGE, HQ),
                PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND.name());
        verifyNoInteractions(supplierOrderQueryRepository);
    }

    @Test
    @DisplayName("등록 시작 일시가 종료 일시보다 늦으면 VALIDATION_ERROR를 던진다")
    void getSupplierOrders_invalidRange() {
        SupplierOrderSearchCondition condition = SupplierOrderSearchCondition.unscoped(
                null, null, null, null, null,
                LocalDateTime.of(2026, 10, 2, 0, 0), LocalDateTime.of(2026, 10, 1, 0, 0));

        assertError(() -> supplierOrderService.getSupplierOrders(condition, PAGE, HQ), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(supplierOrderQueryRepository);
    }

    @Test
    @DisplayName("상세는 헤더와 항목을 합쳐 돌려준다")
    void getSupplierOrder_success() {
        SupplierOrderLineView line = new SupplierOrderLineView(11L, 1L, "SKU-1", "상품", "EA", 100L, 0L, 100L,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(100000), PurchaseOrderLineStatus.REQUESTED);
        when(supplierOrderQueryRepository.findView(21L)).thenReturn(Optional.of(view(1L)));
        when(supplierOrderQueryRepository.findLineViews(21L)).thenReturn(List.of(line));

        SupplierOrderDetails details = supplierOrderService.getSupplierOrder(21L, MANAGER);

        assertThat(details.header().supplierOrderNo()).isEqualTo("SO-20261015-0001");
        assertThat(details.lines()).containsExactly(line);
    }

    @Test
    @DisplayName("없는 발주서는 SUPPLIER_ORDER_NOT_FOUND, 다른 창고 발주서는 항목을 조회하지 않고 403이다")
    void getSupplierOrder_notFoundAndForbidden() {
        when(supplierOrderQueryRepository.findView(999L)).thenReturn(Optional.empty());
        when(supplierOrderQueryRepository.findView(21L)).thenReturn(Optional.of(view(9L)));

        assertError(() -> supplierOrderService.getSupplierOrder(999L, HQ),
                SupplierOrderErrorCode.SUPPLIER_ORDER_NOT_FOUND.name());
        assertError(() -> supplierOrderService.getSupplierOrder(21L, MANAGER), ErrorCode.FORBIDDEN.name());
        verify(supplierOrderQueryRepository, org.mockito.Mockito.never()).findLineViews(any());
    }
}
