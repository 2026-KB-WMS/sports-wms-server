package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.application.port.out.SectionCapacityPort;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class InventoryStockServiceTest {

    @Mock
    private InventoryLotRepository inventoryLotRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private LotRepository lotRepository;
    @Mock
    private SectionCapacityPort sectionCapacityPort;

    @InjectMocks
    private InventoryStockService inventoryStockService;

    private static InventoryLot lotWith(Long inventoryLotId, long sectionId, long lotId, long onHand, long allocated) {
        InventoryLot inventoryLot = InventoryLot.open(sectionId, lotId, QualityStatus.AVAILABLE);
        if (onHand > 0) {
            inventoryLot.increase(onHand);
        }
        if (allocated > 0) {
            inventoryLot.allocate(allocated);
        }
        ReflectionTestUtils.setField(inventoryLot, "inventoryLotId", inventoryLotId);
        return inventoryLot;
    }

    // ---------- receive ----------

    @Test
    @DisplayName("기존 재고 행이 없으면 새로 만들어 보유 수량을 늘리고 구역 사용 용량을 채운다")
    void receive_newInventoryLot_success() {
        StockReceiveCommand command = new StockReceiveCommand(10L, 5L, QualityStatus.AVAILABLE, 50L, 1L, 9L);
        Lot lot = Lot.register(1L, 1L, "LOT-001", null, null, BigDecimal.TEN);
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        when(inventoryLotRepository.findBySectionIdAndLotIdForUpdate(10L, 5L)).thenReturn(Optional.empty());
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryLot> result = inventoryStockService.receive(List.of(command));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOnHandQuantity()).isEqualTo(50L);
        verify(sectionCapacityPort).occupy(10L, 50L);
        verify(inventoryTransactionRepository).save(any());
    }

    @Test
    @DisplayName("입고는 재고 행을 찾기(잠그기) 전에 관련 구역 행을 먼저 잠근다 (동시 첫 입고 시 재고 행 중복 생성 방지)")
    void receive_locksSectionsBeforeInventoryRows() {
        StockReceiveCommand first = new StockReceiveCommand(20L, 5L, QualityStatus.AVAILABLE, 10L, 1L, 9L);
        StockReceiveCommand second = new StockReceiveCommand(10L, 5L, QualityStatus.AVAILABLE, 10L, 1L, 9L);
        Lot lot = Lot.register(1L, 1L, "LOT-001", null, null, BigDecimal.TEN);
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        when(inventoryLotRepository.findBySectionIdAndLotIdForUpdate(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryStockService.receive(List.of(first, second));

        InOrder inOrder = inOrder(sectionCapacityPort, inventoryLotRepository);
        inOrder.verify(sectionCapacityPort).lock(List.of(10L, 20L));
        inOrder.verify(inventoryLotRepository).findBySectionIdAndLotIdForUpdate(10L, 5L);
        inOrder.verify(inventoryLotRepository).findBySectionIdAndLotIdForUpdate(20L, 5L);
    }

    @Test
    @DisplayName("입고 수량이 0 이하이면 VALIDATION_ERROR 예외를 던진다")
    void receive_quantityNotPositive() {
        StockReceiveCommand command = new StockReceiveCommand(10L, 5L, QualityStatus.AVAILABLE, 0L, 1L, 9L);

        assertThatThrownBy(() -> inventoryStockService.receive(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(lotRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 로트로 입고하면 NOT_FOUND 예외를 던진다")
    void receive_lotNotFound() {
        StockReceiveCommand command = new StockReceiveCommand(10L, 999L, QualityStatus.AVAILABLE, 50L, 1L, 9L);
        when(lotRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryStockService.receive(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("가용 상태가 아닌 로트로 입고하면 LOT_NOT_AVAILABLE 예외를 던진다")
    void receive_lotNotAvailable() {
        StockReceiveCommand command = new StockReceiveCommand(10L, 5L, QualityStatus.AVAILABLE, 50L, 1L, 9L);
        Lot lot = Lot.register(1L, 1L, "LOT-001", null, null, BigDecimal.TEN);
        lot.quarantine();
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> inventoryStockService.receive(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_AVAILABLE.name());
    }

    @Test
    @DisplayName("같은 구역·로트의 기존 재고와 품질 상태가 다르면 CONFLICT 예외를 던진다")
    void receive_qualityStatusMismatch() {
        StockReceiveCommand command = new StockReceiveCommand(10L, 5L, QualityStatus.AVAILABLE, 50L, 1L, 9L);
        Lot lot = Lot.register(1L, 1L, "LOT-001", null, null, BigDecimal.TEN);
        InventoryLot existing = InventoryLot.open(10L, 5L, QualityStatus.DEFECTIVE);
        when(lotRepository.findById(5L)).thenReturn(Optional.of(lot));
        when(inventoryLotRepository.findBySectionIdAndLotIdForUpdate(10L, 5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> inventoryStockService.receive(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(sectionCapacityPort, never()).occupy(any(), anyLong());
    }

    // ---------- allocate ----------

    @Test
    @DisplayName("가용 수량이 충분하면 할당에 성공한다")
    void allocate_success() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 0L);
        StockQuantityCommand command = new StockQuantityCommand(1L, 30L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryLot> result = inventoryStockService.allocate(List.of(command));

        assertThat(result.get(0).getAllocatedQuantity()).isEqualTo(30L);
    }

    @Test
    @DisplayName("존재하지 않는 재고 행을 할당하려 하면 NOT_FOUND 예외를 던진다")
    void allocate_inventoryNotFound() {
        StockQuantityCommand command = new StockQuantityCommand(999L, 30L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> inventoryStockService.allocate(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("가용 수량이 부족하면 INSUFFICIENT_STOCK 예외를 던진다")
    void allocate_insufficientStock() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 10L, 0L);
        StockQuantityCommand command = new StockQuantityCommand(1L, 30L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));

        assertThatThrownBy(() -> inventoryStockService.allocate(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.INSUFFICIENT_STOCK.name());
        verify(inventoryLotRepository, never()).save(any());
    }

    @Test
    @DisplayName("할당 수량이 0 이하이면 VALIDATION_ERROR 예외를 던진다")
    void allocate_quantityNotPositive() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 0L);
        StockQuantityCommand command = new StockQuantityCommand(1L, 0L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));

        assertThatThrownBy(() -> inventoryStockService.allocate(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    // ---------- release ----------

    @Test
    @DisplayName("할당 수량 이내이면 해제에 성공한다")
    void release_success() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 30L);
        StockQuantityCommand command = new StockQuantityCommand(1L, 20L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryLot> result = inventoryStockService.release(List.of(command));

        assertThat(result.get(0).getAllocatedQuantity()).isEqualTo(10L);
    }

    @Test
    @DisplayName("할당 수량보다 많이 해제하려 하면 CONFLICT 예외를 던진다")
    void release_exceedsAllocated() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 10L);
        StockQuantityCommand command = new StockQuantityCommand(1L, 20L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));

        assertThatThrownBy(() -> inventoryStockService.release(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(inventoryLotRepository, never()).save(any());
    }

    // ---------- ship ----------

    @Test
    @DisplayName("피킹 수량만큼 보유·할당 수량을 차감하고 구역 사용 용량을 비운다")
    void ship_fullyPicked_success() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 30L);
        StockShipCommand command = new StockShipCommand(1L, 30L, 30L, 7L, 9L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryLot> result = inventoryStockService.ship(List.of(command));

        assertThat(result.get(0).getOnHandQuantity()).isEqualTo(70L);
        assertThat(result.get(0).getAllocatedQuantity()).isEqualTo(0L);
        verify(sectionCapacityPort).vacate(10L, 30L);
        verify(inventoryTransactionRepository).save(any());
    }

    @Test
    @DisplayName("할당 수량보다 적게 피킹하면 부족분은 해제되고 피킹분만 출고 처리된다")
    void ship_shortPicked_releasesShortage() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 30L);
        StockShipCommand command = new StockShipCommand(1L, 30L, 20L, 7L, 9L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryLot> result = inventoryStockService.ship(List.of(command));

        assertThat(result.get(0).getOnHandQuantity()).isEqualTo(80L);
        assertThat(result.get(0).getAllocatedQuantity()).isEqualTo(0L);
        verify(sectionCapacityPort).vacate(10L, 20L);
    }

    @Test
    @DisplayName("피킹 수량이 할당 수량보다 크면 VALIDATION_ERROR 예외를 던진다")
    void ship_pickedExceedsAllocated() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 30L);
        StockShipCommand command = new StockShipCommand(1L, 30L, 40L, 7L, 9L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));

        assertThatThrownBy(() -> inventoryStockService.ship(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("재고의 할당 수량이 요청한 출고 할당 수량보다 적으면 CONFLICT 예외를 던진다")
    void ship_insufficientAllocated() {
        InventoryLot lot = lotWith(1L, 10L, 5L, 100L, 10L);
        StockShipCommand command = new StockShipCommand(1L, 30L, 30L, 7L, 9L);
        when(inventoryLotRepository.findAllByIdsForUpdate(List.of(1L))).thenReturn(List.of(lot));

        assertThatThrownBy(() -> inventoryStockService.ship(List.of(command)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
    }
}
