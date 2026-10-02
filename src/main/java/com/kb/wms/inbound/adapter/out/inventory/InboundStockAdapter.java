package com.kb.wms.inbound.adapter.out.inventory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.InboundStockPort;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;
import com.kb.wms.inventory.domain.enums.QualityStatus;

import lombok.RequiredArgsConstructor;

/**
 * 입고 도메인의 재고 반영 포트를 재고 도메인 유스케이스에 연결한다.
 */
@Component
@RequiredArgsConstructor
public class InboundStockAdapter implements InboundStockPort {

    private final InventoryStockUseCase inventoryStockUseCase;

    @Override
    public List<ReceivedStock> receive(Long inboundId, Long userId, List<StockReceipt> receipts) {
        List<StockReceiveCommand> commands = receipts.stream()
                .map(receipt -> new StockReceiveCommand(
                        receipt.sectionId(), receipt.lotId(),
                        receipt.defective() ? QualityStatus.DEFECTIVE : QualityStatus.AVAILABLE,
                        receipt.quantity(), inboundId, userId))
                .toList();
        return inventoryStockUseCase.receive(commands).stream()
                .map(lot -> new ReceivedStock(lot.getSectionId(), lot.getLotId(),
                        lot.getQualityStatus() == QualityStatus.DEFECTIVE, lot.getInventoryLotId()))
                .toList();
    }
}
