package com.kb.wms.store.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.kb.wms.store.domain.entity.StoreMember;

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
@Table(name = "store_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_member_store_user",
                columnNames = {"store_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_member_id")
    private Long storeMemberId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

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
    private StoreMemberJpaEntity(Long storeMemberId, Long storeId, Long userId, String memberRole,
                                 LocalDateTime assignedAt) {
        this.storeMemberId = storeMemberId;
        this.storeId = storeId;
        this.userId = userId;
        this.memberRole = memberRole;
        this.assignedAt = assignedAt;
    }

    public static StoreMemberJpaEntity fromDomain(StoreMember member) {
        return StoreMemberJpaEntity.builder()
                .storeMemberId(member.getStoreMemberId())
                .storeId(member.getStoreId())
                .userId(member.getUserId())
                .memberRole(member.getMemberRole())
                .assignedAt(member.getAssignedAt())
                .build();
    }

    public StoreMember toDomain() {
        return StoreMember.builder()
                .storeMemberId(storeMemberId)
                .storeId(storeId)
                .userId(userId)
                .memberRole(memberRole)
                .assignedAt(assignedAt)
                .createdAt(createdAt)
                .build();
    }
}
