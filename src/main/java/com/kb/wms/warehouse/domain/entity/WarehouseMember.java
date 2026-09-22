package com.kb.wms.warehouse.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자와 창고의 소속·업무 역할을 연결하는 매핑.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseMember {

    private Long warehouseMemberId;
    private Long warehouseId;
    private Long userId;
    private String memberRole;
    private LocalDateTime assignedAt;
    private LocalDateTime createdAt;

    @Builder
    private WarehouseMember(Long warehouseMemberId, Long warehouseId, Long userId, String memberRole,
                             LocalDateTime assignedAt, LocalDateTime createdAt) {
        this.warehouseMemberId = warehouseMemberId;
        this.warehouseId = warehouseId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.assignedAt = assignedAt;
        this.createdAt = createdAt;
    }

    public static WarehouseMember assign(Long warehouseId, Long userId, String memberRole,
                                          LocalDateTime assignedAt) {
        return WarehouseMember.builder()
                .warehouseId(warehouseId)
                .userId(userId)
                .memberRole(memberRole)
                .assignedAt(assignedAt)
                .build();
    }
}
