package com.kb.wms.inventory.application.port.in.command;

import com.kb.wms.inventory.domain.enums.LotStatus;

/**
 * PATCH /api/v1/lots/{lotId}/status.
 *
 * @param status 목표 상태 (AVAILABLE·QUARANTINED·DISPOSED)
 * @param reason 변경 사유 (필수, 500자 이하). 상태 이력에 남긴다.
 * @param userId 처리 사용자 (상태 이력 changed_by)
 */
public record LotStatusChangeCommand(
        LotStatus status,
        String reason,
        Long userId
) {
}
