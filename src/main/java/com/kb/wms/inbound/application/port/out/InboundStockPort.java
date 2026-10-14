package com.kb.wms.inbound.application.port.out;

import java.util.List;

/**
 * 재고 도메인에 입고 수량을 반영하기 위한 아웃바운드 포트.
 * 재고 행 가산·생성, 구역 사용 용량 증가, 재고 이력 기록은 재고 도메인이 호출자의 트랜잭션 안에서 처리한다.
 */
public interface InboundStockPort {

    /**
     * 구역 + 로트별 입고 수량을 재고에 반영하고 반영된 재고 행 ID를 돌려준다.
     * 구역 비활성·수용량 초과·로트 가용 불가는 재고·창고 도메인 오류(409)로 던져지며 트랜잭션 전체가 롤백된다.
     */
    List<ReceivedStock> receive(Long inboundId, Long userId, List<StockReceipt> receipts);

    /** 입고가 재고에 반영하는 수량의 품질. 재고 도메인의 품질 상태와는 어댑터에서만 서로 변환한다. */
    enum StockQuality {
        AVAILABLE,
        DEFECTIVE
    }

    record StockReceipt(Long sectionId, Long lotId, StockQuality quality, long quantity) {
    }

    record ReceivedStock(Long sectionId, Long lotId, StockQuality quality, Long inventoryLotId) {
    }
}
