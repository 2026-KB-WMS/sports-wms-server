package com.kb.wms.warehouse.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.domain.entity.Inbound;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.enums.InboundStatus;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;
import com.kb.wms.warehouse.application.port.in.WarehouseUseCase;
import com.kb.wms.warehouse.application.port.in.command.WarehouseRegisterCommand;
import com.kb.wms.warehouse.domain.entity.Warehouse;
import com.kb.wms.warehouse.exception.WarehouseErrorCode;

/**
 * 창고 비활성화와 진행 중 업무(입고·발주·출고·지점 발주)의 통합 검증(실제 빈 연결: 창고 → 읽기 전용 조회 어댑터).
 * 진행 중 상태가 하나라도 있으면 WAREHOUSE_IN_USE, 종결 상태만 있으면 허용한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class WarehouseDeactivationWithOperationsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Autowired WarehouseUseCase warehouseUseCase;
    @Autowired InboundRepository inboundRepository;
    @Autowired PurchaseOrderRepository purchaseOrderRepository;
    @Autowired OutboundRepository outboundRepository;
    @Autowired StoreOrderRepository storeOrderRepository;

    @Test
    @DisplayName("진행 중 입고(ARRIVED·INSPECTING)가 있으면 창고를 비활성화할 수 없다")
    void inProgressInbound_blocks() {
        for (InboundStatus status : new InboundStatus[] {InboundStatus.ARRIVED, InboundStatus.INSPECTING}) {
            Warehouse warehouse = newWarehouse("WH-IB-" + status);
            inbound("IB-" + status, warehouse.getWarehouseId(), status);

            assertInUse(warehouse, status.name());
        }
    }

    @Test
    @DisplayName("진행 중 창고 발주(REQUESTED·CONFIRMED)가 있으면 창고를 비활성화할 수 없다")
    void inProgressPurchaseOrder_blocks() {
        for (PurchaseOrderStatus status : new PurchaseOrderStatus[] {
                PurchaseOrderStatus.REQUESTED, PurchaseOrderStatus.CONFIRMED}) {
            Warehouse warehouse = newWarehouse("WH-PO-" + status);
            purchaseOrder("PO-" + status, warehouse.getWarehouseId(), status);

            assertInUse(warehouse, status.name());
        }
    }

    @Test
    @DisplayName("진행 중 출고(READY·PICKING·PICKED·SHIPPED)가 있으면 창고를 비활성화할 수 없다")
    void inProgressOutbound_blocks() {
        for (OutboundStatus status : new OutboundStatus[] {
                OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED}) {
            Warehouse warehouse = newWarehouse("WH-OB-" + status);
            // 출고는 창고를 직접 갖지 않으므로 배정 창고가 이 창고인 지점 발주(종결 상태)에 연결한다.
            StoreOrder order = storeOrder("SO-OB-" + status, warehouse.getWarehouseId(), StoreOrderStatus.COMPLETED);
            outbound("OB-" + status, order.getStoreOrderId(), status);

            assertInUse(warehouse, status.name());
        }
    }

    @Test
    @DisplayName("이 창고에 배정된 진행 중 지점 발주가 있으면 창고를 비활성화할 수 없다")
    void inProgressStoreOrder_blocks() {
        for (StoreOrderStatus status : new StoreOrderStatus[] {
                StoreOrderStatus.APPROVED, StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD}) {
            Warehouse warehouse = newWarehouse("WH-SO-" + status);
            storeOrder("SO-" + status, warehouse.getWarehouseId(), status);

            assertInUse(warehouse, status.name());
        }
    }

    @Test
    @DisplayName("종결된 입고·발주·출고·지점 발주만 있으면 창고를 비활성화할 수 있다")
    void closedOperationsOnly_allowDeactivation() {
        Warehouse warehouse = newWarehouse("WH-CLOSED");
        Long id = warehouse.getWarehouseId();
        inbound("IB-C1", id, InboundStatus.COMPLETED);
        inbound("IB-C2", id, InboundStatus.CANCELED);
        purchaseOrder("PO-C1", id, PurchaseOrderStatus.COMPLETED);
        purchaseOrder("PO-C2", id, PurchaseOrderStatus.CANCELED);
        StoreOrder done = storeOrder("SO-C1", id, StoreOrderStatus.COMPLETED);
        storeOrder("SO-C2", id, StoreOrderStatus.CANCELED);
        outbound("OB-C1", done.getStoreOrderId(), OutboundStatus.DELIVERED);
        outbound("OB-C2", done.getStoreOrderId(), OutboundStatus.CANCELED);

        Warehouse result = warehouseUseCase.deactivateWarehouse(id);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("다른 창고의 진행 중 업무는 영향을 주지 않는다")
    void otherWarehouseOperations_doNotBlock() {
        Warehouse target = newWarehouse("WH-TARGET");
        Warehouse other = newWarehouse("WH-OTHER");
        inbound("IB-O", other.getWarehouseId(), InboundStatus.ARRIVED);
        purchaseOrder("PO-O", other.getWarehouseId(), PurchaseOrderStatus.CONFIRMED);
        storeOrder("SO-O", other.getWarehouseId(), StoreOrderStatus.ASSIGNED);

        assertThat(warehouseUseCase.deactivateWarehouse(target.getWarehouseId()).isActive()).isFalse();
    }

    private void assertInUse(Warehouse warehouse, String label) {
        assertThatThrownBy(() -> warehouseUseCase.deactivateWarehouse(warehouse.getWarehouseId()))
                .as(label)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_IN_USE.name());
    }

    private Warehouse newWarehouse(String code) {
        return warehouseUseCase.registerWarehouse(
                new WarehouseRegisterCommand(code, code, "주소", "02-1234-5678", BigDecimal.TEN));
    }

    private void inbound(String no, Long warehouseId, InboundStatus status) {
        inboundRepository.save(Inbound.builder()
                .inboundNo(no).purchaseOrderId(1L).warehouseId(warehouseId).status(status).arrivedAt(NOW)
                .build());
    }

    private void purchaseOrder(String no, Long warehouseId, PurchaseOrderStatus status) {
        purchaseOrderRepository.save(PurchaseOrder.builder()
                .purchaseOrderNo(no).warehouseId(warehouseId).supplierId(1L).status(status)
                .expectedAt(NOW.plusDays(3)).createdBy(1L)
                .build());
    }

    private StoreOrder storeOrder(String no, Long warehouseId, StoreOrderStatus status) {
        return storeOrderRepository.save(StoreOrder.builder()
                .orderNo(no).storeId(1L).warehouseId(warehouseId).status(status)
                .requestedAt(NOW).createdBy(1L)
                .build());
    }

    private void outbound(String no, Long storeOrderId, OutboundStatus status) {
        outboundRepository.save(Outbound.builder()
                .outboundNo(no).storeOrderId(storeOrderId).status(status)
                .build());
    }
}
