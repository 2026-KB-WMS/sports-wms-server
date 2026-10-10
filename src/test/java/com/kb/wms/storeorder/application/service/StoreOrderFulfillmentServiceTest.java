package com.kb.wms.storeorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLinePickedCommand;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.entity.StoreOrderLine;
import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

@ExtendWith(MockitoExtension.class)
class StoreOrderFulfillmentServiceTest {

    @Mock StoreOrderRepository storeOrderRepository;
    @Mock StatusHistoryUseCase statusHistoryUseCase;
    @InjectMocks StoreOrderFulfillmentService service;

    private StoreOrder order(StoreOrderStatus status) {
        return StoreOrder.builder()
                .storeOrderId(1L)
                .orderNo("SO-20261005-0001")
                .storeId(1L)
                .warehouseId(1L)
                .status(status)
                .requestedAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .createdBy(1L)
                .build();
    }

    private StoreOrderLine line(long id, long requested, long allocated, long shipped) {
        return StoreOrderLine.builder()
                .storeOrderLineId(id)
                .storeOrderId(1L)
                .skuId(id + 100)
                .requestedQuantity(requested)
                .allocatedQuantity(allocated)
                .shippedQuantity(shipped)
                .requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(StoreOrderLineStatus.REQUESTED)
                .build();
    }

    @Test
    @DisplayName("없는 발주를 조회하면 STORE_ORDER_NOT_FOUND")
    void getOrderNotFound() {
        when(storeOrderRepository.findById(1L)).thenReturn(Optional.empty());
        when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getOrderForUpdate(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("할당 수량을 늘리면 항목을 ID 오름차순으로 잠가 반영한다")
    void increaseAllocated() {
        StoreOrderLine l1 = line(1, 10, 0, 0);
        StoreOrderLine l2 = line(2, 5, 0, 0);
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(l1, l2));

        service.increaseAllocated(List.of(
                new StoreOrderLineQuantityCommand(2L, 5),
                new StoreOrderLineQuantityCommand(1L, 4),
                new StoreOrderLineQuantityCommand(1L, 6)));

        assertThat(l1.getAllocatedQuantity()).isEqualTo(10);
        assertThat(l2.getAllocatedQuantity()).isEqualTo(5);
        verify(storeOrderRepository).saveLines(anyList());
    }

    @Test
    @DisplayName("잔여 수량을 넘겨 할당하면 예외이며 저장하지 않는다")
    void increaseAllocatedOverRemaining() {
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L))).thenReturn(List.of(line(1, 10, 8, 0)));

        assertThatThrownBy(() -> service.increaseAllocated(List.of(new StoreOrderLineQuantityCommand(1L, 3))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(storeOrderRepository, never()).saveLines(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 항목이 있으면 예외")
    void missingLine() {
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(line(1, 10, 10, 0)));

        assertThatThrownBy(() -> service.decreaseAllocated(List.of(
                new StoreOrderLineQuantityCommand(1L, 1), new StoreOrderLineQuantityCommand(2L, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("할당 수량을 줄인다")
    void decreaseAllocated() {
        StoreOrderLine l = line(1, 10, 10, 0);
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L))).thenReturn(List.of(l));

        service.decreaseAllocated(List.of(new StoreOrderLineQuantityCommand(1L, 10)));

        assertThat(l.getAllocatedQuantity()).isZero();
        verify(storeOrderRepository).saveLines(anyList());
    }

    @Test
    @DisplayName("피킹 완료 반영 시 할당은 줄고 출고는 늘어난다")
    void applyPicked() {
        StoreOrderLine l = line(1, 10, 10, 0);
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L))).thenReturn(List.of(l));

        service.applyPicked(List.of(new StoreOrderLinePickedCommand(1L, 10, 7)));

        assertThat(l.getAllocatedQuantity()).isZero();
        assertThat(l.getShippedQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("항목 상태를 출고 수량 기준으로 다시 계산한다")
    void refreshLineStatuses() {
        StoreOrderLine partial = line(1, 10, 0, 4);
        StoreOrderLine full = line(2, 5, 0, 5);
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L, 2L))).thenReturn(List.of(partial, full));

        service.refreshLineStatuses(List.of(2L, 1L));

        assertThat(partial.getStatus()).isEqualTo(StoreOrderLineStatus.PARTIALLY_SHIPPED);
        assertThat(full.getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
    }

    @Test
    @DisplayName("모든 항목이 전량 출고되면 발주를 COMPLETED 로 바꾸고 이력을 남긴다")
    void completeIfFulfilled() {
        StoreOrder order = order(StoreOrderStatus.ASSIGNED);
        when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(storeOrderRepository.findLinesByStoreOrderId(1L))
                .thenReturn(List.of(line(1, 10, 0, 10), line(2, 5, 0, 6)));
        when(storeOrderRepository.save(order)).thenReturn(order);

        StoreOrder result = service.completeIfFulfilled(1L, 9L);

        assertThat(result.getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.STORE_ORDER, 1L,
                "ASSIGNED", "COMPLETED", null, 9L);
    }

    @Test
    @DisplayName("남은 수량이 있으면 발주 상태를 바꾸지 않는다")
    void completeIfFulfilledRemaining() {
        StoreOrder order = order(StoreOrderStatus.ASSIGNED);
        when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(storeOrderRepository.findLinesByStoreOrderId(1L))
                .thenReturn(List.of(line(1, 10, 0, 10), line(2, 5, 0, 3)));

        StoreOrder result = service.completeIfFulfilled(1L, 9L);

        assertThat(result.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        verify(storeOrderRepository, never()).save(any());
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("ASSIGNED 가 아닌 발주는 바꾸지 않는다")
    void completeIfFulfilledNotAssigned() {
        StoreOrder order = order(StoreOrderStatus.ON_HOLD);
        when(storeOrderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        StoreOrder result = service.completeIfFulfilled(1L, 9L);

        assertThat(result.getStatus()).isEqualTo(StoreOrderStatus.ON_HOLD);
        verify(storeOrderRepository, never()).findLinesByStoreOrderId(eq(1L));
    }

    @Test
    @DisplayName("항목 잠금은 중복 ID를 한 번만 요청한다")
    void lockDistinctIds() {
        StoreOrderLine l = line(1, 10, 0, 0);
        when(storeOrderRepository.findLinesByIdsForUpdate(List.of(1L))).thenReturn(List.of(l));
        ArgumentCaptor<List<StoreOrderLine>> saved = ArgumentCaptor.forClass(List.class);

        service.increaseAllocated(List.of(new StoreOrderLineQuantityCommand(1L, 3),
                new StoreOrderLineQuantityCommand(1L, 3)));

        verify(storeOrderRepository).saveLines(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(l.getAllocatedQuantity()).isEqualTo(6);
    }
}
