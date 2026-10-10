package com.kb.wms.statushistory.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

class StatusHistoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

    @Test
    @DisplayName("최초 생성 이력은 fromStatus가 null이고 isInitial이 true다")
    void record_initial() {
        StatusHistory history = StatusHistory.record(
                StatusHistoryEntityType.STORE_ORDER, 1L, null, "REQUESTED", null, 10L, NOW);

        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo("REQUESTED");
        assertThat(history.getChangedBy()).isEqualTo(10L);
        assertThat(history.getChangedAt()).isEqualTo(NOW);
        assertThat(history.isInitial()).isTrue();
    }

    @Test
    @DisplayName("상태 변경 이력은 이전·이후 상태와 사유를 담는다")
    void record_transition() {
        StatusHistory history = StatusHistory.record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "REJECTED", "재고 부족", 10L, NOW);

        assertThat(history.getFromStatus()).isEqualTo("REQUESTED");
        assertThat(history.getToStatus()).isEqualTo("REJECTED");
        assertThat(history.getReason()).isEqualTo("재고 부족");
        assertThat(history.isInitial()).isFalse();
    }

    @Test
    @DisplayName("빈 사유는 null로, 앞뒤 공백은 제거해 저장한다")
    void record_normalizesReason() {
        StatusHistory blank = StatusHistory.record(
                StatusHistoryEntityType.INBOUND, 1L, "ARRIVED", "CANCELED", "   ", 1L, NOW);
        StatusHistory padded = StatusHistory.record(
                StatusHistoryEntityType.INBOUND, 1L, "ARRIVED", "CANCELED", "  파손  ", 1L, NOW);

        assertThat(blank.getReason()).isNull();
        assertThat(padded.getReason()).isEqualTo("파손");
    }

    @Test
    @DisplayName("사유가 500자를 넘으면 예외를 던진다")
    void record_reasonTooLong() {
        String tooLong = "가".repeat(StatusHistory.MAX_REASON_LENGTH + 1);

        assertThatThrownBy(() -> StatusHistory.record(
                StatusHistoryEntityType.INBOUND, 1L, "ARRIVED", "CANCELED", tooLong, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("사유가 정확히 500자면 허용한다")
    void record_reasonMaxLength() {
        String max = "가".repeat(StatusHistory.MAX_REASON_LENGTH);

        StatusHistory history = StatusHistory.record(
                StatusHistoryEntityType.INBOUND, 1L, "ARRIVED", "CANCELED", max, 1L, NOW);

        assertThat(history.getReason()).hasSize(StatusHistory.MAX_REASON_LENGTH);
    }

    @Test
    @DisplayName("entityType, entityId, toStatus가 없으면 예외를 던진다")
    void record_requiredFields() {
        assertThatThrownBy(() -> StatusHistory.record(null, 1L, null, "A", null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StatusHistory.record(StatusHistoryEntityType.OUTBOUND, null, null, "A", null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StatusHistory.record(StatusHistoryEntityType.OUTBOUND, 1L, null, " ", null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StatusHistory.record(StatusHistoryEntityType.OUTBOUND, 1L, null, null, null, 1L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("처리자가 없으면 예외를 던진다 (시스템 자동 전이도 트리거한 사용자가 필요하다)")
    void record_requiresChangedBy() {
        assertThatThrownBy(() -> StatusHistory.record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "APPROVED", null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
