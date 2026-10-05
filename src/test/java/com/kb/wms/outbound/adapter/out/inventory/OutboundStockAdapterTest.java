package com.kb.wms.outbound.adapter.out.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockPick;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockQuantity;
import com.kb.wms.outbound.application.port.out.OutboundStockPort.StockState;

@ExtendWith(MockitoExtension.class)
class OutboundStockAdapterTest {

    @Mock InventoryStockUseCase inventoryStockUseCase;
    @InjectMocks OutboundStockAdapter adapter;

    private InventoryLot lot(Long id, long onHand, long allocated) {
        return InventoryLot.builder()
                .inventoryLotId(id).sectionId(1L).lotId(1L)
                .onHandQuantity(onHand).allocatedQuantity(allocated)
                .qualityStatus(QualityStatus.AVAILABLE).build();
    }

    @Test
    @DisplayName("할당은 재고 유스케이스 명령으로 변환하고 반영된 수량을 돌려준다")
    void allocate() {
        when(inventoryStockUseCase.allocate(List.of(new StockQuantityCommand(5L, 4))))
                .thenReturn(List.of(lot(5L, 10, 4)));

        List<StockState> states = adapter.allocate(List.of(new StockQuantity(5L, 4)));

        assertThat(states).containsExactly(new StockState(5L, 10, 4));
        assertThat(states.get(0).availableQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("해제는 재고 유스케이스의 해제로 연결된다")
    void release() {
        when(inventoryStockUseCase.release(List.of(new StockQuantityCommand(5L, 4))))
                .thenReturn(List.of(lot(5L, 10, 0)));

        assertThat(adapter.release(List.of(new StockQuantity(5L, 4))))
                .containsExactly(new StockState(5L, 10, 0));
    }

    @Test
    @DisplayName("피킹 완료는 출고 ID와 처리자를 담아 차감 명령으로 변환한다")
    void ship() {
        StockShipCommand command = new StockShipCommand(5L, 10, 7, 100L, 9L);
        when(inventoryStockUseCase.ship(List.of(command))).thenReturn(List.of(lot(5L, 3, 0)));

        List<StockState> states = adapter.ship(100L, 9L, List.of(new StockPick(5L, 10, 7)));

        verify(inventoryStockUseCase).ship(List.of(command));
        assertThat(states).containsExactly(new StockState(5L, 3, 0));
    }
}
