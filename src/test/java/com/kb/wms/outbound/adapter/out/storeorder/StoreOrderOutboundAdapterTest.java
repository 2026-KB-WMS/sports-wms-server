package com.kb.wms.outbound.adapter.out.storeorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.outbound.application.port.out.OutboundRepository;
import com.kb.wms.outbound.application.port.out.StockAllocationRepository;
import com.kb.wms.outbound.domain.entity.Outbound;
import com.kb.wms.outbound.domain.entity.StockAllocation;
import com.kb.wms.outbound.domain.enums.AllocationStatus;
import com.kb.wms.outbound.domain.enums.OutboundStatus;
import com.kb.wms.storeorder.application.port.in.StoreOrderFulfillmentUseCase;
import com.kb.wms.storeorder.application.port.in.command.StoreOrderLineQuantityCommand;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;

@ExtendWith(MockitoExtension.class)
class StoreOrderOutboundAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);

    @Mock OutboundRepository outboundRepository;
    @Mock StockAllocationRepository stockAllocationRepository;
    @Mock InventoryStockUseCase inventoryStockUseCase;
    @Mock StoreOrderFulfillmentUseCase storeOrderFulfillmentUseCase;
    @Mock StatusHistoryUseCase statusHistoryUseCase;
    @InjectMocks StoreOrderOutboundAdapter adapter;

    private Outbound outbound(Long id, String no, Long storeOrderId, OutboundStatus status) {
        return Outbound.builder().outboundId(id).outboundNo(no).storeOrderId(storeOrderId).status(status).build();
    }

    private StockAllocation allocation(Long id, Long lineId, Long lotId, long quantity) {
        return StockAllocation.builder().allocationId(id).storeOrderLineId(lineId).inventoryLotId(lotId)
                .allocatedQuantity(quantity).status(AllocationStatus.ALLOCATED).allocatedAt(NOW).build();
    }

    @Test
    @DisplayName("가장 최근 출고의 상태를 돌려주고 출고가 없으면 비어 있다")
    void latestStatus() {
        when(outboundRepository.findByStoreOrderId(1L)).thenReturn(List.of(
                outbound(1L, "OB-1", 1L, OutboundStatus.CANCELED), outbound(2L, "OB-2", 1L, OutboundStatus.PICKING)));
        when(outboundRepository.findByStoreOrderId(2L)).thenReturn(List.of());

        assertThat(adapter.findLatestOutboundStatus(1L)).contains(StoreOrderOutboundStatus.PICKING);
        assertThat(adapter.findLatestOutboundStatus(2L)).isEmpty();
    }

    @Test
    @DisplayName("여러 발주의 최근 출고 상태를 한 번에 조회하고 출고가 없는 발주는 키가 없다")
    void latestStatuses() {
        when(outboundRepository.findByStoreOrderIds(Set.of(1L, 2L, 3L))).thenReturn(List.of(
                outbound(1L, "OB-1", 1L, OutboundStatus.CANCELED),
                outbound(2L, "OB-2", 2L, OutboundStatus.DELIVERED),
                outbound(3L, "OB-3", 1L, OutboundStatus.READY)));

        Map<Long, StoreOrderOutboundStatus> result = adapter.findLatestOutboundStatuses(Set.of(1L, 2L, 3L));

        assertThat(result).containsEntry(1L, StoreOrderOutboundStatus.READY)
                .containsEntry(2L, StoreOrderOutboundStatus.DELIVERED)
                .doesNotContainKey(3L);
    }

    @Test
    @DisplayName("발주의 출고 목록을 생성 순으로 변환한다(취소 포함)")
    void findOutbounds() {
        Outbound shipped = outbound(2L, "OB-2", 1L, OutboundStatus.SHIPPED);
        when(outboundRepository.findByStoreOrderId(1L)).thenReturn(List.of(
                outbound(1L, "OB-1", 1L, OutboundStatus.CANCELED), shipped));

        List<StoreOrderOutboundView> views = adapter.findOutbounds(1L);

        assertThat(views).extracting(StoreOrderOutboundView::outboundNo).containsExactly("OB-1", "OB-2");
        assertThat(views.get(0).status()).isEqualTo(StoreOrderOutboundStatus.CANCELED);
    }

    @Test
    @DisplayName("피킹 시작·진행 중 판단은 상태 집합으로 조회한다")
    void existsByStatuses() {
        when(outboundRepository.existsByStoreOrderIdAndStatusIn(1L, Set.of(
                OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED, OutboundStatus.DELIVERED)))
                .thenReturn(true);
        when(outboundRepository.existsByStoreOrderIdAndStatusIn(1L, Set.of(
                OutboundStatus.READY, OutboundStatus.PICKING, OutboundStatus.PICKED, OutboundStatus.SHIPPED)))
                .thenReturn(false);

        assertThat(adapter.existsPickingStarted(1L)).isTrue();
        assertThat(adapter.existsInProgressOutbound(1L)).isFalse();
    }

    @Test
    @DisplayName("ALLOCATED 할당이나 취소되지 않은 출고가 있으면 이행 진행 중이다")
    void activeFulfillment() {
        when(stockAllocationRepository.existsByStoreOrderIdAndStatus(1L, AllocationStatus.ALLOCATED)).thenReturn(false);
        when(outboundRepository.existsNotCanceledByStoreOrderId(1L)).thenReturn(true);
        when(stockAllocationRepository.existsByStoreOrderIdAndStatus(2L, AllocationStatus.ALLOCATED)).thenReturn(true);
        when(stockAllocationRepository.existsByStoreOrderIdAndStatus(3L, AllocationStatus.ALLOCATED)).thenReturn(false);
        when(outboundRepository.existsNotCanceledByStoreOrderId(3L)).thenReturn(false);

        assertThat(adapter.existsActiveFulfillment(1L)).isTrue();
        assertThat(adapter.existsActiveFulfillment(2L)).isTrue();
        assertThat(adapter.existsActiveFulfillment(3L)).isFalse();
    }

    @Test
    @DisplayName("발주 취소 시 READY 출고를 취소하고 ALLOCATED 할당을 해제하며 재고·발주 항목 수량을 줄인다")
    void cancelFulfillment() {
        Outbound ready = outbound(10L, "OB-1", 1L, OutboundStatus.READY);
        StockAllocation a1 = allocation(20L, 100L, 5L, 6);
        StockAllocation a2 = allocation(21L, 100L, 5L, 4);
        StockAllocation a3 = allocation(22L, 101L, 7L, 3);
        when(outboundRepository.findByStoreOrderIdAndStatusForUpdate(1L, OutboundStatus.READY))
                .thenReturn(List.of(ready));
        when(stockAllocationRepository.findByStoreOrderIdAndStatusForUpdate(1L, AllocationStatus.ALLOCATED))
                .thenReturn(List.of(a1, a2, a3));

        StoreOrderFulfillmentCancelResult result = adapter.cancelFulfillment(1L, 9L);

        assertThat(result.canceledOutboundCount()).isEqualTo(1);
        assertThat(result.releasedAllocationCount()).isEqualTo(3);
        assertThat(ready.getStatus()).isEqualTo(OutboundStatus.CANCELED);
        assertThat(List.of(a1, a2, a3)).allSatisfy(a -> {
            assertThat(a.getStatus()).isEqualTo(AllocationStatus.RELEASED);
            assertThat(a.getReleasedAt()).isNotNull();
        });
        verify(outboundRepository).save(ready);

        ArgumentCaptor<List<StockQuantityCommand>> stock = ArgumentCaptor.forClass(List.class);
        verify(inventoryStockUseCase).release(stock.capture());
        assertThat(stock.getValue()).containsExactly(new StockQuantityCommand(5L, 10), new StockQuantityCommand(7L, 3));

        ArgumentCaptor<List<StoreOrderLineQuantityCommand>> lines = ArgumentCaptor.forClass(List.class);
        verify(storeOrderFulfillmentUseCase).decreaseAllocated(lines.capture());
        assertThat(lines.getValue()).containsExactly(
                new StoreOrderLineQuantityCommand(100L, 10), new StoreOrderLineQuantityCommand(101L, 3));

        verify(statusHistoryUseCase).record(StatusHistoryEntityType.OUTBOUND, 10L, "READY", "CANCELED",
                "발주 취소로 인한 자동 처리", 9L);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.STOCK_ALLOCATION, 20L, "ALLOCATED", "RELEASED",
                "발주 취소로 인한 자동 처리", 9L);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.STOCK_ALLOCATION, 22L, "ALLOCATED", "RELEASED",
                "발주 취소로 인한 자동 처리", 9L);
    }

    @Test
    @DisplayName("정리할 출고·할당이 없으면 재고와 발주 항목을 건드리지 않고 0건을 돌려준다")
    void cancelFulfillmentNothing() {
        when(outboundRepository.findByStoreOrderIdAndStatusForUpdate(1L, OutboundStatus.READY)).thenReturn(List.of());
        when(stockAllocationRepository.findByStoreOrderIdAndStatusForUpdate(1L, AllocationStatus.ALLOCATED))
                .thenReturn(List.of());

        StoreOrderFulfillmentCancelResult result = adapter.cancelFulfillment(1L, 9L);

        assertThat(result).isEqualTo(StoreOrderFulfillmentCancelResult.NONE);
        verifyNoInteractions(inventoryStockUseCase, storeOrderFulfillmentUseCase, statusHistoryUseCase);
        verify(outboundRepository, never()).save(any());
        verify(stockAllocationRepository, never()).save(any());
    }
}
