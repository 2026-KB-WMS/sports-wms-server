package com.kb.wms.storeorder.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

class StoreOrderTest {

    private static final Long WAREHOUSE_ID = 7L;

    private StoreOrder requested() {
        return StoreOrder.register("SO-20261004-0001", 1L, LocalDateTime.of(2026, 10, 4, 9, 0),
                LocalDateTime.of(2026, 10, 7, 9, 0), "정기 보충", 5L);
    }

    private StoreOrder approved() {
        StoreOrder order = requested();
        order.approve();
        return order;
    }

    private StoreOrder assigned() {
        StoreOrder order = approved();
        order.assign(WAREHOUSE_ID);
        return order;
    }

    private StoreOrder withStatus(StoreOrderStatus status) {
        return switch (status) {
            case REQUESTED -> requested();
            case APPROVED -> approved();
            case ASSIGNED -> assigned();
            case ON_HOLD -> {
                StoreOrder order = assigned();
                order.hold();
                yield order;
            }
            case COMPLETED -> {
                StoreOrder order = assigned();
                order.complete();
                yield order;
            }
            case CANCELED -> {
                StoreOrder order = requested();
                order.cancel();
                yield order;
            }
            case REJECTED -> {
                StoreOrder order = requested();
                order.reject();
                yield order;
            }
        };
    }

    private Set<StoreOrderStatus> allExcept(StoreOrderStatus... allowed) {
        Set<StoreOrderStatus> others = EnumSet.allOf(StoreOrderStatus.class);
        for (StoreOrderStatus status : allowed) {
            others.remove(status);
        }
        return others;
    }

    @Test
    @DisplayName("등록하면 REQUESTED이고 창고는 아직 정해지지 않는다")
    void register_initialState() {
        StoreOrder order = requested();

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.REQUESTED);
        assertThat(order.getWarehouseId()).isNull();
        assertThat(order.getOrderNo()).isEqualTo("SO-20261004-0001");
        assertThat(order.getStoreId()).isEqualTo(1L);
        assertThat(order.getCreatedBy()).isEqualTo(5L);
        assertThat(order.isRequested()).isTrue();
    }

    @Test
    @DisplayName("REQUESTED 발주를 승인하면 APPROVED가 되고 창고는 여전히 없다")
    void approve_fromRequested() {
        StoreOrder order = requested();

        order.approve();

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.APPROVED);
        assertThat(order.getWarehouseId()).isNull();
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 승인하면 예외가 발생한다")
    void approve_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.REQUESTED)) {
            StoreOrder order = withStatus(status);

            assertThatThrownBy(order::approve).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("REQUESTED 발주를 반려하면 REJECTED가 된다")
    void reject_fromRequested() {
        StoreOrder order = requested();

        order.reject();

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.REJECTED);
    }

    @Test
    @DisplayName("REQUESTED가 아닌 발주를 반려하면 예외가 발생한다")
    void reject_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.REQUESTED)) {
            StoreOrder order = withStatus(status);

            assertThatThrownBy(order::reject).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @ParameterizedTest
    @EnumSource(value = StoreOrderStatus.class, names = {"REQUESTED", "APPROVED", "ASSIGNED", "ON_HOLD"})
    @DisplayName("진행 중인 발주는 취소할 수 있다")
    void cancel_inProgress(StoreOrderStatus status) {
        StoreOrder order = withStatus(status);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.CANCELED);
    }

    @ParameterizedTest
    @EnumSource(value = StoreOrderStatus.class, names = {"COMPLETED", "CANCELED", "REJECTED"})
    @DisplayName("종결된 발주를 취소하면 예외가 발생한다")
    void cancel_terminal_throws(StoreOrderStatus status) {
        StoreOrder order = withStatus(status);

        assertThatThrownBy(order::cancel).isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("APPROVED 발주에 창고를 배정하면 ASSIGNED가 되고 warehouseId가 채워진다")
    void assign_fromApproved() {
        StoreOrder order = approved();

        order.assign(WAREHOUSE_ID);

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(order.getWarehouseId()).isEqualTo(WAREHOUSE_ID);
    }

    @Test
    @DisplayName("APPROVED가 아닌 발주에 창고를 배정하면 예외가 발생한다 (REQUESTED 직행 포함)")
    void assign_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.APPROVED)) {
            StoreOrder order = withStatus(status);
            Long before = order.getWarehouseId();

            assertThatThrownBy(() -> order.assign(99L)).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
            assertThat(order.getWarehouseId()).isEqualTo(before);
        }
    }

    @Test
    @DisplayName("창고 없이 배정하면 예외가 발생하고 상태는 그대로다")
    void assign_nullWarehouse_throws() {
        StoreOrder order = approved();

        assertThatThrownBy(() -> order.assign(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.APPROVED);
    }

    @Test
    @DisplayName("ASSIGNED 발주를 다른 창고로 재배정하면 상태는 유지되고 창고만 바뀐다")
    void reassign_fromAssigned() {
        StoreOrder order = assigned();

        order.reassign(8L);

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(order.getWarehouseId()).isEqualTo(8L);
    }

    @Test
    @DisplayName("같은 창고로 재배정하면 예외가 발생한다")
    void reassign_sameWarehouse_throws() {
        StoreOrder order = assigned();

        assertThatThrownBy(() -> order.reassign(WAREHOUSE_ID)).isInstanceOf(IllegalArgumentException.class);
        assertThat(order.getWarehouseId()).isEqualTo(WAREHOUSE_ID);
    }

    @Test
    @DisplayName("ASSIGNED가 아닌 발주는 재배정할 수 없다 (ON_HOLD 포함)")
    void reassign_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.ASSIGNED)) {
            StoreOrder order = withStatus(status);
            Long before = order.getWarehouseId();

            assertThatThrownBy(() -> order.reassign(99L)).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
            assertThat(order.getWarehouseId()).isEqualTo(before);
        }
    }

    @Test
    @DisplayName("ASSIGNED 발주를 보류하면 ON_HOLD가 되고 재개하면 ASSIGNED로 돌아온다")
    void hold_and_resume() {
        StoreOrder order = assigned();

        order.hold();
        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.ON_HOLD);

        order.resume();
        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.ASSIGNED);
        assertThat(order.getWarehouseId()).isEqualTo(WAREHOUSE_ID);
    }

    @Test
    @DisplayName("ASSIGNED가 아닌 발주를 보류하면 예외가 발생한다")
    void hold_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.ASSIGNED)) {
            StoreOrder order = withStatus(status);

            assertThatThrownBy(order::hold).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("ON_HOLD가 아닌 발주를 재개하면 예외가 발생한다")
    void resume_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.ON_HOLD)) {
            StoreOrder order = withStatus(status);

            assertThatThrownBy(order::resume).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("ASSIGNED 발주를 종결하면 COMPLETED가 된다")
    void complete_fromAssigned() {
        StoreOrder order = assigned();

        order.complete();

        assertThat(order.getStatus()).isEqualTo(StoreOrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("ASSIGNED가 아닌 발주를 종결하면 예외가 발생한다 (보류 중 포함)")
    void complete_fromOtherStatus_throws() {
        for (StoreOrderStatus status : allExcept(StoreOrderStatus.ASSIGNED)) {
            StoreOrder order = withStatus(status);

            assertThatThrownBy(order::complete).isInstanceOf(IllegalStateException.class);
            assertThat(order.getStatus()).isEqualTo(status);
        }
    }

    @ParameterizedTest
    @EnumSource(StoreOrderStatus.class)
    @DisplayName("진행 중인 발주는 REQUESTED·APPROVED·ASSIGNED·ON_HOLD뿐이고 나머지는 종결이다")
    void isInProgress_and_isTerminal(StoreOrderStatus status) {
        boolean expected = status == StoreOrderStatus.REQUESTED || status == StoreOrderStatus.APPROVED
                || status == StoreOrderStatus.ASSIGNED || status == StoreOrderStatus.ON_HOLD;

        StoreOrder order = withStatus(status);

        assertThat(order.isInProgress()).isEqualTo(expected);
        assertThat(order.isTerminal()).isEqualTo(!expected);
    }
}
