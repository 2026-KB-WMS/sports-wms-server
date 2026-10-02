package com.kb.wms.inbound.application.port.in;

import com.kb.wms.inbound.application.port.in.result.InboundCompleteResult;

/**
 * 입고 완료 유스케이스. PATCH /api/v1/inbounds/{inboundId}/complete
 */
public interface InboundCompleteUseCase {

    /**
     * 검수가 끝난 입고(INSPECTING)를 완료 처리한다. 재고 반영, 구역 사용량, 재고 이력, 발주 항목·발주 상태,
     * 입고 상태 전환을 한 트랜잭션으로 처리하며 하나라도 실패하면 아무것도 반영하지 않는다.
     *
     * @param userId 처리 사용자(토큰 사용자). 입고의 처리자와 재고 이력의 수행자로 기록한다
     */
    InboundCompleteResult completeInbound(Long inboundId, Long userId);
}
