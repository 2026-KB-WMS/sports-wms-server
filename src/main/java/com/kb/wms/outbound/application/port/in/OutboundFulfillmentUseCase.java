package com.kb.wms.outbound.application.port.in;

import com.kb.wms.outbound.application.port.in.command.OutboundPickingCompleteCommand;
import com.kb.wms.outbound.application.port.in.result.OutboundDeliverResult;
import com.kb.wms.outbound.application.port.in.result.OutboundPickingCompleteResult;
import com.kb.wms.outbound.application.port.in.result.OutboundShipResult;

/**
 * 출고 유스케이스(서비스 C): 피킹 완료, 배송 시작, 배송 완료. 재고 차감과 발주 수량·상태 전환이 들어 있다.
 */
public interface OutboundFulfillmentUseCase {

    /**
     * PICKING 출고의 항목별 피킹 수량을 확정한다. 재고 차감(부족분 예약 해제 포함), 할당 PICKED, 출고 항목 수량·확정 단가,
     * 발주 항목 수량 갱신, 출고 PICKED를 한 트랜잭션으로 처리한다.
     */
    OutboundPickingCompleteResult completePicking(OutboundPickingCompleteCommand command);

    /** PICKED 출고를 SHIPPED로 바꾼다. 재고·할당은 바뀌지 않는다. */
    OutboundShipResult ship(Long outboundId, Long userId);

    /**
     * SHIPPED 출고를 DELIVERED로 바꾸고 발주 항목 상태를 다시 계산한다. 전량 출고되고 진행 중 출고가 없으면 발주를 COMPLETED로 바꾼다.
     */
    OutboundDeliverResult deliver(Long outboundId, Long userId);
}
