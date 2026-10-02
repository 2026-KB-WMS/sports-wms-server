package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.inbound.domain.enums.InboundStatus;

class InboundTest {

    private static Inbound inbound(InboundStatus status) {
        return Inbound.builder()
                .inboundId(7L)
                .inboundNo("IB-20261002-0001")
                .purchaseOrderId(4L)
                .warehouseId(1L)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("등록하면 ARRIVED 상태로 시작한다")
    void register_startsArrived() {
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 10, 2, 9, 0);

        Inbound inbound = Inbound.register("IB-20261002-0001", 4L, 1L, arrivedAt, "비고");

        assertThat(inbound.getStatus()).isEqualTo(InboundStatus.ARRIVED);
        assertThat(inbound.getPurchaseOrderId()).isEqualTo(4L);
        assertThat(inbound.getWarehouseId()).isEqualTo(1L);
        assertThat(inbound.getArrivedAt()).isEqualTo(arrivedAt);
        assertThat(inbound.getReceivedAt()).isNull();
        assertThat(inbound.getReceivedBy()).isNull();
    }

    @Test
    @DisplayName("상태를 주지 않고 만들면 ARRIVED가 기본값이다")
    void builder_defaultStatus() {
        assertThat(Inbound.builder().build().getStatus()).isEqualTo(InboundStatus.ARRIVED);
    }

    @Test
    @DisplayName("검수는 ARRIVED에서 INSPECTING으로 바꾸고, INSPECTING에서 다시 호출해도 상태가 같다(멱등)")
    void inspect_idempotent() {
        Inbound inbound = inbound(InboundStatus.ARRIVED);

        inbound.inspect();
        assertThat(inbound.getStatus()).isEqualTo(InboundStatus.INSPECTING);

        inbound.inspect();
        assertThat(inbound.getStatus()).isEqualTo(InboundStatus.INSPECTING);
    }

    @Test
    @DisplayName("완료·취소된 입고는 검수할 수 없다")
    void inspect_rejectedAfterFinished() {
        for (InboundStatus status : new InboundStatus[] {InboundStatus.COMPLETED, InboundStatus.CANCELED}) {
            assertThatThrownBy(() -> inbound(status).inspect()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("INSPECTING 입고를 완료하면 COMPLETED가 되고 처리자와 시각이 기록된다")
    void complete_success() {
        Inbound inbound = inbound(InboundStatus.INSPECTING);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 10, 2, 11, 30);

        inbound.complete(5L, receivedAt);

        assertThat(inbound.getStatus()).isEqualTo(InboundStatus.COMPLETED);
        assertThat(inbound.getReceivedBy()).isEqualTo(5L);
        assertThat(inbound.getReceivedAt()).isEqualTo(receivedAt);
    }

    @Test
    @DisplayName("INSPECTING이 아닌 입고는 완료할 수 없다")
    void complete_requiresInspecting() {
        for (InboundStatus status : new InboundStatus[] {
                InboundStatus.ARRIVED, InboundStatus.COMPLETED, InboundStatus.CANCELED}) {
            assertThatThrownBy(() -> inbound(status).complete(5L, LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("ARRIVED·INSPECTING 입고는 취소할 수 있다")
    void cancel_success() {
        for (InboundStatus status : new InboundStatus[] {InboundStatus.ARRIVED, InboundStatus.INSPECTING}) {
            Inbound inbound = inbound(status);

            inbound.cancel();

            assertThat(inbound.getStatus()).isEqualTo(InboundStatus.CANCELED);
        }
    }

    @Test
    @DisplayName("완료·취소된 입고는 취소할 수 없다")
    void cancel_rejectedAfterFinished() {
        for (InboundStatus status : new InboundStatus[] {InboundStatus.COMPLETED, InboundStatus.CANCELED}) {
            assertThatThrownBy(() -> inbound(status).cancel()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("검수·취소 가능 여부는 ARRIVED·INSPECTING에서만 true다")
    void inspectableAndCancelable() {
        assertThat(inbound(InboundStatus.ARRIVED).isInspectable()).isTrue();
        assertThat(inbound(InboundStatus.INSPECTING).isInspectable()).isTrue();
        assertThat(inbound(InboundStatus.COMPLETED).isInspectable()).isFalse();
        assertThat(inbound(InboundStatus.CANCELED).isInspectable()).isFalse();
        assertThat(inbound(InboundStatus.ARRIVED).isCancelable()).isTrue();
        assertThat(inbound(InboundStatus.COMPLETED).isCancelable()).isFalse();
    }
}
