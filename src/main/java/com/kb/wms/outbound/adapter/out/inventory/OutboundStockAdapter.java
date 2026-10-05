package com.kb.wms.outbound.adapter.out.inventory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.outbound.application.port.out.OutboundStockPort;

import lombok.RequiredArgsConstructor;

/**
 * 출고 서비스의 재고 반영 포트를 재고 도메인의 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class OutboundStockAdapter implements OutboundStockPort {

    private final InventoryStockUseCase inventoryStockUseCase;

    @Override
    public List<StockState> allocate(List<StockQuantity> quantities) {
        return toStates(inventoryStockUseCase.allocate(toCommands(quantities)));
    }

    @Override
    public List<StockState> release(List<StockQuantity> quantities) {
        return toStates(inventoryStockUseCase.release(toCommands(quantities)));
    }

    @Override
    public List<StockState> ship(Long outboundId, Long userId, List<StockPick> picks) {
        List<StockShipCommand> commands = picks.stream()
                .map(p -> new StockShipCommand(p.inventoryLotId(), p.allocatedQuantity(), p.pickedQuantity(),
                        outboundId, userId))
                .toList();
        return toStates(inventoryStockUseCase.ship(commands));
    }

    private List<StockQuantityCommand> toCommands(List<StockQuantity> quantities) {
        return quantities.stream()
                .map(q -> new StockQuantityCommand(q.inventoryLotId(), q.quantity()))
                .toList();
    }

    private List<StockState> toStates(List<InventoryLot> lots) {
        return lots.stream()
                .map(lot -> new StockState(lot.getInventoryLotId(), lot.getOnHandQuantity(), lot.getAllocatedQuantity()))
                .toList();
    }
}
