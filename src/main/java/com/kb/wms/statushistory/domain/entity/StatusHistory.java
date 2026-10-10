package com.kb.wms.statushistory.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업무 엔티티의 상태가 바뀔 때마다 남기는 이력 한 건. 한 번 기록하면 수정하지 않는다.
 * 엔티티 유형마다 상태 enum이 달라 이전·이후 상태는 enum 이름 문자열로 저장한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {

    public static final int MAX_REASON_LENGTH = 500;

    private Long statusHistoryId;
    private StatusHistoryEntityType entityType;
    private Long entityId;
    /** 최초 생성 기록이면 null. */
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Long changedBy;
    private LocalDateTime changedAt;

    @Builder
    private StatusHistory(Long statusHistoryId, StatusHistoryEntityType entityType, Long entityId,
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

    /**
     * 상태 변경 이력 생성. 최초 생성이면 fromStatus에 null을 넣는다. 처리자(changedBy)는 필수이고, 시스템 자동 전이는 트리거한 사용자를 넣는다. 빈 사유는 null로 정리한다.
     */
    public static StatusHistory record(StatusHistoryEntityType entityType, Long entityId,
                                       String fromStatus, String toStatus, String reason,
                                       Long changedBy, LocalDateTime changedAt) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType은 필수입니다.");
        }
        if (entityId == null) {
            throw new IllegalArgumentException("entityId는 필수입니다.");
        }
        if (toStatus == null || toStatus.isBlank()) {
            throw new IllegalArgumentException("toStatus는 필수입니다.");
        }
        if (changedBy == null) {
            throw new IllegalArgumentException("changedBy는 필수입니다.");
        }
        String normalizedReason = reason == null || reason.isBlank() ? null : reason.strip();
        if (normalizedReason != null && normalizedReason.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("사유는 " + MAX_REASON_LENGTH + "자 이하여야 합니다.");
        }
        return StatusHistory.builder()
                .entityType(entityType)
                .entityId(entityId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(normalizedReason)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
    }

    /** 최초 생성 기록(이전 상태 없음)인지 여부. */
    public boolean isInitial() {
        return fromStatus == null;
    }
}
