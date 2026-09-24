package com.kb.wms.warehouse.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.warehouse.domain.entity.WarehouseMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * updated_at이 없는 배정 이력성 엔티티라 BaseTimeEntity를 상속하지 않는다.
 */
@Entity
@Table(name = "warehouse_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_member_warehouse_user",
                columnNames = {"warehouse_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_member_id")
    private Long warehouseMemberId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "member_role", nullable = false, length = 30)
    private String memberRole;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private WarehouseMemberJpaEntity(Long warehouseMemberId, Long warehouseId, Long userId, String memberRole,
                                      LocalDateTime assignedAt) {
        this.warehouseMemberId = warehouseMemberId;
        this.warehouseId = warehouseId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.assignedAt = assignedAt;
    }

    public static WarehouseMemberJpaEntity fromDomain(WarehouseMember member) {
        return WarehouseMemberJpaEntity.builder()
                .warehouseMemberId(member.getWarehouseMemberId())
                .warehouseId(member.getWarehouseId())
                .userId(member.getUserId())
                .memberRole(member.getMemberRole())
                .assignedAt(member.getAssignedAt())
                .build();
    }

    public WarehouseMember toDomain() {
        return WarehouseMember.builder()
                .warehouseMemberId(warehouseMemberId)
                .warehouseId(warehouseId)
                .userId(userId)
                .memberRole(memberRole)
                .assignedAt(assignedAt)
                .createdAt(createdAt)
                .build();
    }
}
