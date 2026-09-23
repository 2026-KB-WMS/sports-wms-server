package com.kb.wms.inventory.application.port.in;

import java.util.List;

import com.kb.wms.inventory.application.port.in.command.StockQuantityCommand;
import com.kb.wms.inventory.application.port.in.command.StockReceiveCommand;
import com.kb.wms.inventory.application.port.in.command.StockShipCommand;
import com.kb.wms.inventory.domain.entity.InventoryLot;

/**
 * 다른 도메인(입고·출고)이 재고 수량을 바꿀 때 쓰는 유일한 진입점.
 *
 * <p>모두 호출자의 트랜잭션 안에서 실행되며, 대상 재고 행을 비관적 락으로 잠근다.
 * 여러 행을 다룰 때는 교착을 막기 위해 inventory_lot_id 오름차순으로 잠근다.
 * 보유 수량이 바뀌는 receive·ship은 InventoryTransaction을 남기고,
 * 보유 수량이 그대로인 allocate·release는 이력을 남기지 않는다(API 명세 기준).
 */
public interface InventoryStockUseCase {

    /**
     * 입고 완료: 구역 + 로트 재고 행에 보유 수량을 더한다(없으면 생성).
     */
    List<InventoryLot> receive(List<StockReceiveCommand> commands);

    /**
     * 재고 할당(예약): 가용 수량에서 할당 수량을 늘린다. 한 행이라도 부족하면 전체 실패.
     */
    List<InventoryLot> allocate(List<StockQuantityCommand> commands);

    /**
     * 할당 해제: 할당 수량을 줄인다.
     */
    List<InventoryLot> release(List<StockQuantityCommand> commands);

    /**
     * 피킹 완료: 보유 수량을 피킹 수량만큼, 할당 수량을 할당 전체만큼 줄인다.
     */
    List<InventoryLot> ship(List<StockShipCommand> commands);
}
