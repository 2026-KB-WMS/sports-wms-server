package com.kb.wms.outbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kb.wms.outbound.domain.enums.OutboundStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboundTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);

    private Outbound newOutbound() {
        return Outbound.create("OUT-20261005-001", 1L, "메모");
    }

    private Outbound picked() {
        Outbound o = newOutbound();
        o.startPicking();
        o.completePicking();
        return o;
    }

    @Test
    @DisplayName("생성 시 READY 상태")
    void create() {
        Outbound o = newOutbound();

        assertThat(o.getStatus()).isEqualTo(OutboundStatus.READY);
        assertThat(o.isPickingStarted()).isFalse();
        assertThat(o.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("출고 번호/발주가 없으면 생성 실패")
    void createInvalid() {
        assertThatThrownBy(() -> Outbound.create(" ", 1L, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Outbound.create("OUT-1", null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("정상 흐름: READY → PICKING → PICKED → SHIPPED → DELIVERED")
    void happyPath() {
        Outbound o = newOutbound();

        o.startPicking();
        assertThat(o.getStatus()).isEqualTo(OutboundStatus.PICKING);
        assertThat(o.isPickingStarted()).isTrue();

        o.completePicking();
        assertThat(o.getStatus()).isEqualTo(OutboundStatus.PICKED);

        o.ship(NOW, 7L);
        assertThat(o.getStatus()).isEqualTo(OutboundStatus.SHIPPED);
        assertThat(o.getShippedAt()).isEqualTo(NOW);
        assertThat(o.getShippedBy()).isEqualTo(7L);

        o.deliver(NOW.plusDays(1));
        assertThat(o.getStatus()).isEqualTo(OutboundStatus.DELIVERED);
        assertThat(o.getDeliveredAt()).isEqualTo(NOW.plusDays(1));
        assertThat(o.isTerminal()).isTrue();
        assertThat(o.isInProgress()).isFalse();
    }

    @Test
    @DisplayName("READY 에서만 취소할 수 있다")
    void cancel() {
        Outbound o = newOutbound();
        o.cancel();

        assertThat(o.getStatus()).isEqualTo(OutboundStatus.CANCELED);
        assertThat(o.isTerminal()).isTrue();
        assertThat(o.isPickingStarted()).isFalse();
    }

    @Test
    @DisplayName("PICKING 이후에는 취소할 수 없다")
    void cancelAfterPicking() {
        Outbound o = newOutbound();
        o.startPicking();

        assertThatThrownBy(o::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(picked()::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("잘못된 상태에서의 전이는 예외")
    void invalidTransitions() {
        Outbound ready = newOutbound();
        assertThatThrownBy(ready::completePicking).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ready.ship(NOW, 1L)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ready.deliver(NOW)).isInstanceOf(IllegalStateException.class);

        Outbound picked = picked();
        assertThatThrownBy(picked::startPicking).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> picked.deliver(NOW)).isInstanceOf(IllegalStateException.class);

        Outbound canceled = newOutbound();
        canceled.cancel();
        assertThatThrownBy(canceled::startPicking).isInstanceOf(IllegalStateException.class);
    }
}
