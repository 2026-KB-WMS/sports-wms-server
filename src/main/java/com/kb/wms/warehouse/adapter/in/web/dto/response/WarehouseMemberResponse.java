package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정/조회 응답. User 도메인이 아직 없어 userName 등 사용자 정보는 포함하지 않는다.
 */
public record WarehouseMemberResponse(
        Long warehouseMemberId,
        Long warehouseId,
        String warehouseName,
        Long userId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static WarehouseMemberResponse of(WarehouseMember member, String warehouseName) {
        return new WarehouseMemberResponse(
                member.getWarehouseMemberId(),
                member.getWarehouseId(),
                warehouseName,
                member.getUserId(),
                member.getMemberRole(),
                member.getAssignedAt());
    }
}
