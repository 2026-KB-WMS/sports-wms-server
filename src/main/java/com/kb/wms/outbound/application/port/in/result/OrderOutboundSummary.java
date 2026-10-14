package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 지점 발주에 딸린 출고 한 건의 요약(발주 연동용). 상태는 출고 상태 이름 문자열이다.
 */
public record OrderOutboundSummary(
        Long outboundId,
        String outboundNo,
        String status,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt
) {
}
