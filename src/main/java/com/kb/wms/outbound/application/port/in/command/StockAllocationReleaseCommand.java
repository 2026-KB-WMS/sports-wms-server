package com.kb.wms.outbound.application.port.in.command;

/**
 * 재고 할당 해제 요청(PATCH /api/v1/allocations/{allocationId}/release).
 *
 * @param reason 해제 사유(필수, 500자 이하). 상태 이력에 저장한다.
 */
public record StockAllocationReleaseCommand(
        Long allocationId,
        String reason,
        Long userId
) {
}