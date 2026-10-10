package com.kb.wms.statushistory.application.port.in.result;

import java.time.LocalDateTime;

/**
 * 처리자 이름을 함께 담은 상태 이력 조회 결과. 조회 전용 JPQL이 users를 ID로 조인해 만든다(ADR-007).
 *
 * @param fromStatus    최초 생성 기록이면 null
 * @param changedByName 처리자 사용자가 없으면 null
 */
public record StatusHistoryView(
        String fromStatus,
        String toStatus,
        String reason,
        Long changedBy,
        String changedByName,
        LocalDateTime changedAt
) {
}
