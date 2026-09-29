package com.kb.wms.store.application.port.in.result;

import java.time.LocalDateTime;

/**
 * GET /api/v1/stores/my 응답에 필요한, 지점 정보와 배정(StoreMember) 정보를 함께 담은 요약.
 */
public record StoreMembershipSummary(
        Long storeId,
        String storeCode,
        String name,
        String address,
        String contactName,
        String contactNumber,
        boolean isActive,
        Long storeMemberId,
        String memberRole,
        LocalDateTime assignedAt
) {
}
