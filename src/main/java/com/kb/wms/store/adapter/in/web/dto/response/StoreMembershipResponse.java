package com.kb.wms.store.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.store.application.port.in.result.StoreMembershipSummary;

/**
 * GET /api/v1/stores/my 응답 항목.
 */
public record StoreMembershipResponse(
        Long storeId,
        String storeCode,
        String storeName,
        String address,
        String contactName,
        String contactNumber,
        boolean isActive,
        Long storeMemberId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static StoreMembershipResponse from(StoreMembershipSummary summary) {
        return new StoreMembershipResponse(
                summary.storeId(),
                summary.storeCode(),
                summary.name(),
                summary.address(),
                summary.contactName(),
                summary.contactNumber(),
                summary.isActive(),
                summary.storeMemberId(),
                summary.memberRole(),
                summary.assignedAt());
    }
}
