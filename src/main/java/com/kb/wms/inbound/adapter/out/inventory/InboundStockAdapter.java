package com.kb.wms.inbound.adapter.out.inventory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kb.wms.inbound.application.port.out.InboundStockPort;
import com.kb.wms.inventory.application.port.in.InventoryStockUseCase;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;

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
                        toInventoryQuality(receipt.quality()),
                        receipt.quantity(), inboundId, userId))
                .toList();
        return inventoryStockUseCase.receive(commands).stream()
                .map(lot -> new ReceivedStock(lot.sectionId(), lot.lotId(),
                        toQuality(lot.qualityStatus()), lot.inventoryLotId()))
                .toList();
    }

    /** 재고 도메인이 받는 품질 상태 이름으로 바꾼다. 값이 늘어나면 컴파일러가 이 switch를 잡는다. */
    private static String toInventoryQuality(StockQuality quality) {
        return switch (quality) {
            case AVAILABLE -> "AVAILABLE";
            case DEFECTIVE -> "DEFECTIVE";
        };
    }

    /** 재고 도메인이 돌려준 품질 상태 이름을 입고의 품질로 바꾼다. 모르는 값은 거절한다. */
    private static StockQuality toQuality(String inventoryQuality) {
        return switch (inventoryQuality) {
            case "AVAILABLE" -> StockQuality.AVAILABLE;
            case "DEFECTIVE" -> StockQuality.DEFECTIVE;
            default -> throw new IllegalStateException("알 수 없는 재고 품질 상태: " + inventoryQuality);
        };
    }
}
