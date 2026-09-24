package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.application.port.out.SectionCapacityPort;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {

    @Mock
    private InventoryLotRepository inventoryLotRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    private SectionCapacityPort sectionCapacityPort;

    @InjectMocks
    private InventoryAdjustmentService inventoryAdjustmentService;

    private InventoryLot lot;

    @BeforeEach
    void setUp() {
        lot = InventoryLot.open(10L, 20L, QualityStatus.AVAILABLE);
        lot.increase(100L);
        lot.allocate(20L);
    }

    @Test
    @DisplayName("현재 보유 수량과 beforeQuantity가 일치하고 수량이 늘어나면 구역 사용 용량을 증가시키고 조정에 성공한다")
    void adjust_increase_success() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 100L, 120L, "실사 결과 반영", 99L);
        when(inventoryLotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentResult result = inventoryAdjustmentService.adjust(command);

        assertThat(result.inventoryLot().getOnHandQuantity()).isEqualTo(120L);
        assertThat(result.transaction().getBeforeQuantity()).isEqualTo(100L);
        assertThat(result.transaction().getAfterQuantity()).isEqualTo(120L);
        verify(sectionCapacityPort).occupy(10L, 20L);
        verify(sectionCapacityPort, never()).vacate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("수량이 줄어들면 구역 사용 용량을 줄이고 조정에 성공한다")
    void adjust_decrease_success() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 100L, 80L, "파손 수량 제외", 99L);
        when(inventoryLotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lot));
        when(inventoryLotRepository.save(any(InventoryLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryTransactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryAdjustmentResult result = inventoryAdjustmentService.adjust(command);

        assertThat(result.inventoryLot().getOnHandQuantity()).isEqualTo(80L);
        verify(sectionCapacityPort).vacate(10L, 20L);
        verify(sectionCapacityPort, never()).occupy(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 재고를 조정하면 NOT_FOUND 예외를 던진다")
    void adjust_inventoryNotFound() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(999L, 100L, 120L, "사유", 99L);
        when(inventoryLotRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("beforeQuantity가 현재 보유 수량과 다르면 STALE_QUANTITY 예외를 던진다")
    void adjust_staleQuantity() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 90L, 120L, "사유", 99L);
        when(inventoryLotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.STALE_QUANTITY.name());
        verify(inventoryLotRepository, never()).save(any());
        verify(sectionCapacityPort, never()).occupy(anyLong(), anyLong());
        verify(sectionCapacityPort, never()).vacate(anyLong(), anyLong());
    }

    @Test
    @DisplayName("조정 후 수량이 할당 수량보다 작으면 BELOW_ALLOCATED_QUANTITY 예외를 던진다")
    void adjust_belowAllocatedQuantity() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 100L, 10L, "사유", 99L);
        when(inventoryLotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.BELOW_ALLOCATED_QUANTITY.name());
        verify(inventoryLotRepository, never()).save(any());
    }

    @Test
    @DisplayName("afterQuantity가 null이거나 음수이면 VALIDATION_ERROR 예외를 던진다")
    void adjust_afterQuantityInvalid() {
        InventoryAdjustCommand nullCommand = new InventoryAdjustCommand(1L, 100L, null, "사유", 99L);
        InventoryAdjustCommand negativeCommand = new InventoryAdjustCommand(1L, 100L, -1L, "사유", 99L);

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(nullCommand))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(negativeCommand))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(inventoryLotRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("beforeQuantity가 null이면 VALIDATION_ERROR 예외를 던진다")
    void adjust_beforeQuantityNull() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, null, 120L, "사유", 99L);

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(inventoryLotRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("조정 전후 수량이 같으면 VALIDATION_ERROR 예외를 던진다")
    void adjust_sameQuantity() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 100L, 100L, "사유", 99L);

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("조정 사유가 비어 있으면 VALIDATION_ERROR 예외를 던진다")
    void adjust_reasonBlank() {
        InventoryAdjustCommand command = new InventoryAdjustCommand(1L, 100L, 120L, "  ", 99L);

        assertThatThrownBy(() -> inventoryAdjustmentService.adjust(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(inventoryLotRepository, never()).findByIdForUpdate(anyLong());
    }
}
