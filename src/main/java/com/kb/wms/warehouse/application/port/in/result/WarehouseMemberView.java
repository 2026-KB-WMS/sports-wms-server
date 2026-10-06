package com.kb.wms.warehouse.application.port.in.result;

import java.time.LocalDateTime;

import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정 한 건과 대상 사용자의 이름·로그인 아이디. 창고 이름은 웹 어댑터가 붙인다.
 * 목록은 조회 쿼리가 사용자 테이블을 ID로 조인해 채우고, 배정 직후에는 서비스가 확인한 사용자 정보로 채운다.
 */
public record WarehouseMemberView(
        Long warehouseMemberId,
        Long warehouseId,
        Long userId,
        String userName,
        String loginId,
        String memberRole,
        LocalDateTime assignedAt
) {

    public static WarehouseMemberView of(WarehouseMember member, String userName, String loginId) {
        return new WarehouseMemberView(member.getWarehouseMemberId(), member.getWarehouseId(), member.getUserId(),
                userName, loginId, member.getMemberRole(), member.getAssignedAt());
    }
}
