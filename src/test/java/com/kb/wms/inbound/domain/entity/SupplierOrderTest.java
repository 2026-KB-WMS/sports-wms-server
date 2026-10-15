package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

class SupplierOrderTest {

    private SupplierOrder newOrder() {
        return SupplierOrder.issue("SO-20261015-0001", 4L, 2L);
    }

    @Test
    @DisplayName("발행하면 ISSUED이고 요청·공급처가 기록된다")
    void issue_startsIssued() {
        SupplierOrder order = newOrder();

        assertThat(order.getSupplierOrderNo()).isEqualTo("SO-20261015-0001");
        assertThat(order.getPurchaseOrderId()).isEqualTo(4L);
        assertThat(order.getSupplierId()).isEqualTo(2L);
        assertThat(order.getStatus()).isEqualTo(SupplierOrderStatus.ISSUED);
        assertThat(order.isInProgress()).isTrue();
    }

    @Test
    @DisplayName("번호·요청·공급처가 비어 있으면 발행할 수 없다")
    void issue_rejectsMissingValues() {
        assertThatThrownBy(() -> SupplierOrder.issue(" ", 4L, 2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SupplierOrder.issue("SO-1", null, 2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SupplierOrder.issue("SO-1", 4L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ISSUED에서 일부 입고로 전이하고, 이미 일부 입고면 상태를 유지한다")
    void markPartiallyReceived_isIdempotent() {
        SupplierOrder order = newOrder();

        order.markPartiallyReceived();
        order.markPartiallyReceived();

        assertThat(order.getStatus()).isEqualTo(SupplierOrderStatus.PARTIALLY_RECEIVED);
    }

    @Test
    @DisplayName("ISSUED·PARTIALLY_RECEIVED에서 완료할 수 있다")
    void complete_fromInProgress() {
        SupplierOrder issued = newOrder();
        issued.complete();
        assertThat(issued.getStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
        assertThat(issued.isInProgress()).isFalse();

        SupplierOrder partial = newOrder();
        partial.markPartiallyReceived();
        partial.complete();
        assertThat(partial.getStatus()).isEqualTo(SupplierOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("ISSUED·PARTIALLY_RECEIVED에서 취소할 수 있다")
    void cancel_fromInProgress() {
        SupplierOrder issued = newOrder();
        issued.cancel();
        assertThat(issued.getStatus()).isEqualTo(SupplierOrderStatus.CANCELED);

        SupplierOrder partial = newOrder();
        partial.markPartiallyReceived();
        partial.cancel();
        assertThat(partial.getStatus()).isEqualTo(SupplierOrderStatus.CANCELED);
    }

    @Test
    @DisplayName("완료·취소된 발주서는 더 이상 전이할 수 없다")
    void terminalStates_rejectTransitions() {
        SupplierOrder completed = newOrder();
        completed.complete();
        assertThatThrownBy(completed::complete).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(completed::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(completed::markPartiallyReceived).isInstanceOf(IllegalStateException.class);

        SupplierOrder canceled = newOrder();
        canceled.cancel();
        assertThatThrownBy(canceled::complete).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(canceled::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(canceled::markPartiallyReceived).isInstanceOf(IllegalStateException.class);
    }
}
