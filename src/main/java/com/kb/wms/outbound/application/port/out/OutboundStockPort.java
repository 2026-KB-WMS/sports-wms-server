package com.kb.wms.outbound.application.port.out;

import java.util.List;

/**
 * 재고 행에 할당·해제·출고 차감을 반영하기 위한 아웃바운드 포트.
 * 재고 수량 변경은 재고 도메인의 유스케이스로만 하며, 호출한 출고 서비스의 트랜잭션 안에서 처리한다.
 * 재고 행은 재고 서비스가 inventory_lot_id 오름차순으로 비관적 락을 걸어 처리한다.
 */
public interface OutboundStockPort {

    /** 재고 행의 할당 수량을 늘린다(예약). 한 행이라도 가용 수량이 부족하면 예외로 전체 실패한다. */
    List<StockState> allocate(List<StockQuantity> quantities);

    /** 재고 행의 할당 수량을 줄인다(해제). 보유 수량은 그대로다. */
    List<StockState> release(List<StockQuantity> quantities);

    /**
     * 피킹 완료: 보유 수량을 피킹 수량만큼, 할당 수량을 할당 전체만큼 줄인다. 피킹 수량이 0보다 크면 재고 이력(OUTBOUND)을 남긴다.
     *
     * @param outboundId 재고 이력의 참조 출고 ID
     * @param userId     재고 이력의 처리자
     */
    List<StockState> ship(Long outboundId, Long userId, List<StockPick> picks);

    record StockQuantity(Long inventoryLotId, long quantity) {
    }

    record StockPick(Long inventoryLotId, long allocatedQuantity, long pickedQuantity) {
    }

    /** 반영 뒤 재고 행의 수량. */
    record StockState(Long inventoryLotId, long onHandQuantity, long allocatedQuantity) {

        public long availableQuantity() {
            return onHandQuantity - allocatedQuantity;
        }
    }
}
