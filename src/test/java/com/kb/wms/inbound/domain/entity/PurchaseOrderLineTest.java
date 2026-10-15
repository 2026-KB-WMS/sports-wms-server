package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.inbound.domain.enums.PurchaseOrderLineStatus;

class PurchaseOrderLineTest {

    private PurchaseOrderLine newLine(long expectedQuantity) {
        return PurchaseOrderLine.register(4L, 1L, expectedQuantity, BigDecimal.valueOf(60000));
    }

    @Test
    @DisplayName("항목을 등록하면 REQUESTED·입고 수량 0이고 금액은 수량 × 단가다")
    void register_calculatesLineAmount() {
        PurchaseOrderLine line = newLine(100);

        assertThat(line.getPurchaseOrderId()).isEqualTo(4L);
        assertThat(line.getSkuId()).isEqualTo(1L);
        assertThat(line.getExpectedQuantity()).isEqualTo(100L);
        assertThat(line.getReceivedQuantity()).isZero();
        assertThat(line.getOrderedUnitPrice()).isEqualByComparingTo("60000");
        assertThat(line.getLineAmount()).isEqualByComparingTo("6000000");
        assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.REQUESTED);
        assertThat(line.remainingQuantity()).isEqualTo(100L);
    }

    @Test
    @DisplayName("매입 단가가 0이면 금액 0으로 등록할 수 있다")
    void register_zeroUnitPrice() {
        PurchaseOrderLine line = PurchaseOrderLine.register(4L, 1L, 10, BigDecimal.ZERO);

        assertThat(line.getLineAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("발주 수량이 0 이하면 등록할 수 없다")
    void register_nonPositiveQuantity_throws() {
        assertThatThrownBy(() -> newLine(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newLine(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매입 단가가 없거나 음수면 등록할 수 없다")
    void register_invalidUnitPrice_throws() {
        assertThatThrownBy(() -> PurchaseOrderLine.register(4L, 1L, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PurchaseOrderLine.register(4L, 1L, 10, BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("일부만 입고하면 PARTIALLY_RECEIVED가 되고 남은 수량이 줄어든다")
    void receive_partial() {
        PurchaseOrderLine line = newLine(100);

        line.receive(60);

        assertThat(line.getReceivedQuantity()).isEqualTo(60L);
        assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.PARTIALLY_RECEIVED);
        assertThat(line.remainingQuantity()).isEqualTo(40L);
        assertThat(line.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("입고를 나눠서 누적하다가 발주 수량을 채우면 COMPLETED가 된다")
    void receive_accumulatesUntilCompleted() {
        PurchaseOrderLine line = newLine(100);

        line.receive(60);
        line.receive(40);

        assertThat(line.getReceivedQuantity()).isEqualTo(100L);
        assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.COMPLETED);
        assertThat(line.isCompleted()).isTrue();
        assertThat(line.remainingQuantity()).isZero();
    }

    @Test
    @DisplayName("발주 수량을 넘겨 입고해도 COMPLETED이고 남은 수량은 음수가 되지 않는다")
    void receive_overReceipt_remainingIsNotNegative() {
        PurchaseOrderLine line = newLine(100);

        line.receive(120);

        assertThat(line.getReceivedQuantity()).isEqualTo(120L);
        assertThat(line.getStatus()).isEqualTo(PurchaseOrderLineStatus.COMPLETED);
        assertThat(line.remainingQuantity()).isZero();
    }

    @Test
    @DisplayName("입고 수량이 0 이하면 예외가 발생한다")
    void receive_nonPositiveQuantity_throws() {
        PurchaseOrderLine line = newLine(100);

        assertThatThrownBy(() -> line.receive(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line.receive(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(line.getReceivedQuantity()).isZero();
    }

    @Test
    @DisplayName("항목을 등록하면 공급처는 비어 있고, 입고 전에는 지정과 변경을 할 수 있다")
    void assignSupplier_beforeReceiving() {
        PurchaseOrderLine line = newLine(100);
        assertThat(line.getSupplierId()).isNull();

        line.assignSupplier(3L);
        assertThat(line.getSupplierId()).isEqualTo(3L);

        line.assignSupplier(7L);
        assertThat(line.getSupplierId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("공급처 ID가 없으면 지정할 수 없다")
    void assignSupplier_null_throws() {
        PurchaseOrderLine line = newLine(100);

        assertThatThrownBy(() -> line.assignSupplier(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(line.getSupplierId()).isNull();
    }

    @Test
    @DisplayName("입고가 시작된 항목의 공급처는 바꿀 수 없다")
    void assignSupplier_afterReceiving_throws() {
        PurchaseOrderLine line = newLine(100);
        line.assignSupplier(3L);
        line.receive(10);

        assertThatThrownBy(() -> line.assignSupplier(7L)).isInstanceOf(IllegalStateException.class);
        assertThat(line.getSupplierId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("공급처가 배정된 항목은 공급처 발주서에 연결할 수 있다")
    void attachToSupplierOrder_afterAssigningSupplier() {
        PurchaseOrderLine line = newLine(100);
        line.assignSupplier(3L);

        line.attachToSupplierOrder(9L);

        assertThat(line.getSupplierOrderId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("공급처가 없거나 이미 연결된 항목, 빈 발주서 ID는 연결할 수 없다")
    void attachToSupplierOrder_guards() {
        PurchaseOrderLine noSupplier = newLine(100);
        assertThatThrownBy(() -> noSupplier.attachToSupplierOrder(9L)).isInstanceOf(IllegalStateException.class);

        PurchaseOrderLine attached = newLine(100);
        attached.assignSupplier(3L);
        attached.attachToSupplierOrder(9L);
        assertThatThrownBy(() -> attached.attachToSupplierOrder(10L)).isInstanceOf(IllegalStateException.class);
        assertThat(attached.getSupplierOrderId()).isEqualTo(9L);

        PurchaseOrderLine nullId = newLine(100);
        nullId.assignSupplier(3L);
        assertThatThrownBy(() -> nullId.attachToSupplierOrder(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이미 전량 입고된 항목에 입고하면 예외가 발생한다")
    void receive_afterCompleted_throws() {
        PurchaseOrderLine line = newLine(100);
        line.receive(100);

        assertThatThrownBy(() -> line.receive(1)).isInstanceOf(IllegalStateException.class);
        assertThat(line.getReceivedQuantity()).isEqualTo(100L);
    }
}
