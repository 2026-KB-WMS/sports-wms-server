package com.kb.wms.common.statushistory.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * entity_id는 다형 참조라 연관관계 매핑 없이 ID만 가진다. changed_by도 User 도메인이 없어 ID만 가진다.
 */
@Entity
@Table(name = "status_history",
        indexes = {
                @Index(name = "idx_status_history_entity", columnList = "entity_type, entity_id, changed_at"),
                @Index(name = "idx_status_history_changed_by", columnList = "changed_by")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_history_id")
    private Long statusHistoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private StatusHistoryEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Builder
    private StatusHistoryJpaEntity(Long statusHistoryId, StatusHistoryEntityType entityType, Long entityId,
                                   String fromStatus, String toStatus, String reason, Long changedBy,
                                   LocalDateTime changedAt) {
        this.statusHistoryId = statusHistoryId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public static StatusHistoryJpaEntity fromDomain(StatusHistory history) {
        return StatusHistoryJpaEntity.builder()
                .statusHistoryId(history.getStatusHistoryId())
                .entityType(history.getEntityType())
                .entityId(history.getEntityId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }

    public StatusHistory toDomain() {
        return StatusHistory.builder()
                .statusHistoryId(statusHistoryId)
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
    }
}
