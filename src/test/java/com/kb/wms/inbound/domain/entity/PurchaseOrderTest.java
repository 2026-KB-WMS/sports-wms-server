package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

class PurchaseOrderTest {

    private PurchaseOrder requested() {
        return PurchaseOrder.register("PO-20261002-0001", 1L, 3L,
                LocalDateTime.of(2026, 10, 5, 9, 0), "정기 보충 발주", 5L);
    }

    private PurchaseOrder confirmed() {
        PurchaseOrder purchaseOrder = requested();
        purchaseOrder.confirm();
        return purchaseOrder;
    }

    private PurchaseOrder withStatus(PurchaseOrderStatus status) {
        return switch (status) {
            case REQUESTED -> requested();
            case CONFIRMED -> confirmed();
            case COMPLETED -> {
                PurchaseOrder purchaseOrder = confirmed();
                purchaseOrder.complete();
                yield purchaseOrder;
            }
            case CANCELED -> {
                PurchaseOrder purchaseOrder = requested();
                purchaseOrder.cancel();
                yield purchaseOrder;
            }
        };
    }

    @Test
    @DisplayName("발주를 등록하면 REQUESTED 상태로 만들어진다")
    void register_createsRequestedPurchaseOrder() {
        PurchaseOrder purchaseOrder = requested();

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
        assertThat(purchaseOrder.getPurchaseOrderNo()).isEqualTo("PO-20261002-0001");
        assertThat(purchaseOrder.getWarehouseId()).isEqualTo(1L);
        assertThat(purchaseOrder.getSupplierId()).isEqualTo(3L);
        assertThat(purchaseOrder.getCreatedBy()).isEqualTo(5L);
        assertThat(purchaseOrder.isRequested()).isTrue();
    }

    @Test
    @DisplayName("상태를 지정하지 않고 복원하면 REQUESTED가 기본값이다")
    void builder_defaultsToRequested() {
        PurchaseOrder purchaseOrder = PurchaseOrder.builder().purchaseOrderNo("PO-X").build();

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.REQUESTED);
    }

    @Test
    @DisplayName("REQUESTED 발주를 확정하면 CONFIRMED가 된다")
    void confirm_fromRequested() {
        PurchaseOrder purchaseOrder = requested();

        purchaseOrder.confirm();

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
        assertThat(purchaseOrder.isConfirmed()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"CONFIRMED", "COMPLETED", "CANCELED"})
    @DisplayName("REQUESTED가 아닌 발주를 확정하면 예외가 발생한다")
    void confirm_fromOtherStatus_throws(PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = withStatus(status);

        assertThatThrownBy(purchaseOrder::confirm).isInstanceOf(IllegalStateException.class);
        assertThat(purchaseOrder.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"REQUESTED", "CONFIRMED"})
    @DisplayName("REQUESTED·CONFIRMED 발주는 취소할 수 있다")
    void cancel_fromInProgress(PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = withStatus(status);

        purchaseOrder.cancel();

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELED);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"COMPLETED", "CANCELED"})
    @DisplayName("완료·취소된 발주를 취소하면 예외가 발생한다")
    void cancel_fromFinished_throws(PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = withStatus(status);

        assertThatThrownBy(purchaseOrder::cancel).isInstanceOf(IllegalStateException.class);
        assertThat(purchaseOrder.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("CONFIRMED 발주를 완료 처리하면 COMPLETED가 된다")
    void complete_fromConfirmed() {
        PurchaseOrder purchaseOrder = confirmed();

        purchaseOrder.complete();

        assertThat(purchaseOrder.getStatus()).isEqualTo(PurchaseOrderStatus.COMPLETED);
    }

    @ParameterizedTest
    @EnumSource(value = PurchaseOrderStatus.class, names = {"REQUESTED", "COMPLETED", "CANCELED"})
    @DisplayName("CONFIRMED가 아닌 발주를 완료 처리하면 예외가 발생한다")
    void complete_fromOtherStatus_throws(PurchaseOrderStatus status) {
        PurchaseOrder purchaseOrder = withStatus(status);

        assertThatThrownBy(purchaseOrder::complete).isInstanceOf(IllegalStateException.class);
        assertThat(purchaseOrder.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(PurchaseOrderStatus.class)
    @DisplayName("진행 중인 발주는 REQUESTED·CONFIRMED뿐이다")
    void isInProgress(PurchaseOrderStatus status) {
        boolean expected = status == PurchaseOrderStatus.REQUESTED || status == PurchaseOrderStatus.CONFIRMED;

        assertThat(withStatus(status).isInProgress()).isEqualTo(expected);
    }
}
