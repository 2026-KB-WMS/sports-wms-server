package com.kb.wms.storeorder.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

class StoreOrderProgressStageTest {

    @ParameterizedTest
    @EnumSource(StoreOrderOutboundStatus.class)
    @NullSource
    @DisplayName("승인 전·후, 반려, 취소, 보류는 출고 상태와 상관없이 발주 상태만으로 정해진다")
    void statusOnlyStages(StoreOrderOutboundStatus latest) {
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.REQUESTED, latest, false))
                .isEqualTo(StoreOrderProgressStage.PENDING_APPROVAL);
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.REJECTED, latest, false))
                .isEqualTo(StoreOrderProgressStage.REJECTED);
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.CANCELED, latest, false))
                .isEqualTo(StoreOrderProgressStage.CANCELED);
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.APPROVED, latest, false))
                .isEqualTo(StoreOrderProgressStage.AWAITING_ASSIGNMENT);
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.ON_HOLD, latest, false))
                .isEqualTo(StoreOrderProgressStage.ON_HOLD);
    }

    @Test
    @DisplayName("ASSIGNED이고 출고가 없으면 상품 준비 중이다 (출고 도메인 연동 전에도 이 값이 나온다)")
    void assigned_noOutbound_preparing() {
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.ASSIGNED, null, false))
                .isEqualTo(StoreOrderProgressStage.PREPARING);
    }

    @ParameterizedTest
    @EnumSource(value = StoreOrderOutboundStatus.class, names = {"READY", "PICKING", "PICKED", "CANCELED"})
    @DisplayName("ASSIGNED이고 최근 출고가 준비·피킹 단계거나 취소됐으면 상품 준비 중이다")
    void assigned_warehouseSideStages_preparing(StoreOrderOutboundStatus latest) {
        assertThat(StoreOrderProgressStage.resolve(StoreOrderStatus.ASSIGNED, latest, false))
                .isEqualTo(StoreOrderProgressStage.PREPARING);
    }

    @Test
    @DisplayName("ASSIGNED이고 최근 출고가 SHIPPED면 배송 중이다")
    void assigned_shipped_inTransit() {
        assertThat(StoreOrderProgressStage.resolve(
                StoreOrderStatus.ASSIGNED, StoreOrderOutboundStatus.SHIPPED, false))
                .isEqualTo(StoreOrderProgressStage.IN_TRANSIT);
    }

    @Test
    @DisplayName("ASSIGNED이고 최근 출고가 DELIVERED면 일부 배송 완료다 (전량 배송 완료는 COMPLETED로 전환되므로)")
    void assigned_delivered_partiallyDelivered() {
        assertThat(StoreOrderProgressStage.resolve(
                StoreOrderStatus.ASSIGNED, StoreOrderOutboundStatus.DELIVERED, false))
                .isEqualTo(StoreOrderProgressStage.PARTIALLY_DELIVERED);
    }

    @Test
    @DisplayName("COMPLETED는 부족 수량이 없으면 배송 완료, 있으면 일부 수량만 배송 완료다")
    void completed_dependsOnShortage() {
        assertThat(StoreOrderProgressStage.resolve(
                StoreOrderStatus.COMPLETED, StoreOrderOutboundStatus.DELIVERED, false))
                .isEqualTo(StoreOrderProgressStage.COMPLETED);
        assertThat(StoreOrderProgressStage.resolve(
                StoreOrderStatus.COMPLETED, StoreOrderOutboundStatus.DELIVERED, true))
                .isEqualTo(StoreOrderProgressStage.COMPLETED_PARTIAL);
    }

    @Test
    @DisplayName("모든 발주 상태가 어떤 입력에도 단계를 하나 돌려준다")
    void allStatuses_resolved() {
        for (StoreOrderStatus status : StoreOrderStatus.values()) {
            assertThat(StoreOrderProgressStage.resolve(status, null, false)).isNotNull();
        }
    }

    @Test
    @DisplayName("피킹이 시작된 출고 상태와 진행 중 출고 상태를 구분한다")
    void outboundStatusPredicates() {
        assertThat(StoreOrderOutboundStatus.READY.isPickingStarted()).isFalse();
        assertThat(StoreOrderOutboundStatus.PICKING.isPickingStarted()).isTrue();
        assertThat(StoreOrderOutboundStatus.PICKED.isPickingStarted()).isTrue();
        assertThat(StoreOrderOutboundStatus.SHIPPED.isPickingStarted()).isTrue();
        assertThat(StoreOrderOutboundStatus.DELIVERED.isPickingStarted()).isTrue();
        assertThat(StoreOrderOutboundStatus.CANCELED.isPickingStarted()).isFalse();

        assertThat(StoreOrderOutboundStatus.READY.isInProgress()).isTrue();
        assertThat(StoreOrderOutboundStatus.PICKING.isInProgress()).isTrue();
        assertThat(StoreOrderOutboundStatus.PICKED.isInProgress()).isTrue();
        assertThat(StoreOrderOutboundStatus.SHIPPED.isInProgress()).isTrue();
        assertThat(StoreOrderOutboundStatus.DELIVERED.isInProgress()).isFalse();
        assertThat(StoreOrderOutboundStatus.CANCELED.isInProgress()).isFalse();
    }
}
