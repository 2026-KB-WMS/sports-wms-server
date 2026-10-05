package com.kb.wms.outbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kb.wms.outbound.domain.enums.AllocationStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockAllocationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);

    private StockAllocation newAllocation(long quantity) {
        return StockAllocation.allocate(1L, 2L, quantity, 3L, NOW);
    }

    @Test
    @DisplayName("할당 생성 시 ALLOCATED 상태이고 피킹 수량은 0이다")
    void allocate() {
        StockAllocation a = newAllocation(10);

        assertThat(a.getStatus()).isEqualTo(AllocationStatus.ALLOCATED);
        assertThat(a.getAllocatedQuantity()).isEqualTo(10);
        assertThat(a.getPickedQuantity()).isZero();
        assertThat(a.isActive()).isTrue();
    }

    @Test
    @DisplayName("할당 수량이 0 이하면 예외")
    void allocateInvalidQuantity() {
        assertThatThrownBy(() -> newAllocation(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newAllocation(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("해제하면 RELEASED 가 되고 해제 시각이 기록된다")
    void release() {
        StockAllocation a = newAllocation(10);

        a.release(NOW.plusHours(1));

        assertThat(a.getStatus()).isEqualTo(AllocationStatus.RELEASED);
        assertThat(a.getReleasedAt()).isEqualTo(NOW.plusHours(1));
        assertThat(a.isActive()).isFalse();
    }

    @Test
    @DisplayName("이미 해제/피킹된 할당은 다시 해제할 수 없다")
    void releaseTwice() {
        StockAllocation released = newAllocation(10);
        released.release(NOW);
        StockAllocation picked = newAllocation(10);
        picked.pick(10);

        assertThatThrownBy(() -> released.release(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> picked.release(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("피킹 완료 시 PICKED 가 되고 부족 수량이 계산된다")
    void pickWithShortage() {
        StockAllocation a = newAllocation(10);

        a.pick(7);

        assertThat(a.getStatus()).isEqualTo(AllocationStatus.PICKED);
        assertThat(a.getPickedQuantity()).isEqualTo(7);
        assertThat(a.shortageQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("피킹 수량이 0 미만이거나 할당 수량 초과면 예외")
    void pickOutOfRange() {
        StockAllocation a = newAllocation(10);

        assertThatThrownBy(() -> a.pick(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> a.pick(11)).isInstanceOf(IllegalArgumentException.class);
        assertThat(a.getStatus()).isEqualTo(AllocationStatus.ALLOCATED);
    }

    @Test
    @DisplayName("피킹 수량 0 도 허용된다")
    void pickZero() {
        StockAllocation a = newAllocation(10);

        a.pick(0);

        assertThat(a.getStatus()).isEqualTo(AllocationStatus.PICKED);
        assertThat(a.shortageQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("ALLOCATED 가 아니면 피킹할 수 없다")
    void pickInvalidStatus() {
        StockAllocation a = newAllocation(10);
        a.release(NOW);

        assertThatThrownBy(() -> a.pick(5)).isInstanceOf(IllegalStateException.class);
    }
}
