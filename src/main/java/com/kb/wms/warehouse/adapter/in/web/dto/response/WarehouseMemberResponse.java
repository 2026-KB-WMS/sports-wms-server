package com.kb.wms.warehouse.adapter.in.web.dto.response;

import java.time.LocalDateTime;

import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;

/**
 * 창고 관리자 배정/조회 응답. 사용자 정보는 이름·로그인 아이디만 포함한다(비밀번호 등 민감 정보 제외).
 */
public record WarehouseMemberResponse(
        Long warehouseMemberId,
        Long warehouseId,
        String warehouseName,
        Long userId,
        String userName,
        String loginId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static WarehouseMemberResponse of(WarehouseMemberView member, String warehouseName) {
        return new WarehouseMemberResponse(
                member.warehouseMemberId(),
                member.warehouseId(),
                warehouseName,
                member.userId(),
                member.userName(),
                member.loginId(),
                member.memberRole(),
                member.assignedAt());
    }
}
