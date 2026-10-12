package com.kb.wms.outbound.application.port.in.command;

import java.util.List;

/**
 * 출고 생성 요청(POST /api/v1/outbounds).
 *
 * @param storeOrderId 출고할 발주(필수)
 * @param note         비고(선택, 500자 이하)
 * @param skuIds       출고에 묶을 SKU. null이면 연결되지 않은 할당 전부, 비어 있으면 거절한다.
 */
public record OutboundCreateCommand(
        Long storeOrderId,
        String note,
        Long userId,
        List<Long> skuIds
) {

    /** 연결되지 않은 할당 전부를 한 출고로 묶는다. */
    public OutboundCreateCommand(Long storeOrderId, String note, Long userId) {
        this(storeOrderId, note, userId, null);
    }
}
