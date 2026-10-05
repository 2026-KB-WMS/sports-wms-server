package com.kb.wms.outbound.application.port.in.command;

/**
 * 출고 생성 요청(POST /api/v1/outbounds).
 *
 * @param storeOrderId 출고할 발주(필수)
 * @param note         비고(선택, 500자 이하)
 */
public record OutboundCreateCommand(
        Long storeOrderId,
        String note,
        Long userId
) {
}
