package com.kb.wms.outbound.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.outbound.domain.enums.OutboundStatus;

/**
 * 출고 취소 결과(PATCH /api/v1/outbounds/{outboundId}/cancel).
 */
public record OutboundCancelResult(
        Long outboundId,
        String outboundNo,
        OutboundStatus status,
        String cancelReason,
        LocalDateTime updatedAt
) {
}
