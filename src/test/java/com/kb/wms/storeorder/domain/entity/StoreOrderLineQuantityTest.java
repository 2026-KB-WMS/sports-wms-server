package com.kb.wms.storeorder.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

/** 출고 연동으로 바뀌는 발주 항목의 할당·출고 수량과 상태 재계산. */
class StoreOrderLineQuantityTest {

    private StoreOrderLine line(long requested) {
        return StoreOrderLine.register(1L, 2L, requested, new BigDecimal("1000"));
    }

    @Test
    @DisplayName("처음에는 요청 수량 전체가 할당 가능하다")
    void unallocatedInitially() {
        assertThat(line(10).unallocatedQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("할당하면 잔여 수량이 줄고 해제하면 다시 늘어난다")
    void allocateAndRelease() {
        StoreOrderLine l = line(10);

        l.increaseAllocated(6);
        assertThat(l.getAllocatedQuantity()).isEqualTo(6);
        assertThat(l.unallocatedQuantity()).isEqualTo(4);

        l.decreaseAllocated(2);
        assertThat(l.getAllocatedQuantity()).isEqualTo(4);
        assertThat(l.unallocatedQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("잔여 수량을 넘겨 할당하거나 0 이하로 할당하면 예외")
    void allocateInvalid() {
        StoreOrderLine l = line(10);
        l.increaseAllocated(8);

        assertThatThrownBy(() -> l.increaseAllocated(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.increaseAllocated(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(l.getAllocatedQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("할당 수량보다 많이 해제하거나 0 이하로 해제하면 예외")
    void releaseInvalid() {
        StoreOrderLine l = line(10);
        l.increaseAllocated(5);

        assertThatThrownBy(() -> l.decreaseAllocated(6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.decreaseAllocated(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("피킹 완료 시 할당은 할당했던 만큼 줄고 출고는 피킹한 만큼 늘며 부족분은 다시 할당할 수 있다")
    void applyPickedWithShortage() {
        StoreOrderLine l = line(10);
        l.increaseAllocated(10);

        l.applyPicked(10, 7);

        assertThat(l.getAllocatedQuantity()).isZero();
        assertThat(l.getShippedQuantity()).isEqualTo(7);
        assertThat(l.unallocatedQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("피킹 수량이 할당 수량을 넘거나 항목 할당 수량보다 많이 처리하면 예외")
    void applyPickedInvalid() {
        StoreOrderLine l = line(10);
        l.increaseAllocated(5);

        assertThatThrownBy(() -> l.applyPicked(5, 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.applyPicked(5, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> l.applyPicked(6, 6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("전량 출고되면 COMPLETED, 일부면 PARTIALLY_SHIPPED, 출고가 없으면 그대로")
    void refreshStatus() {
        StoreOrderLine none = line(10);
        none.increaseAllocated(10);
        none.applyPicked(10, 0);
        none.refreshStatusByShipped();
        assertThat(none.getStatus()).isEqualTo(StoreOrderLineStatus.REQUESTED);

        StoreOrderLine partial = line(10);
        partial.increaseAllocated(10);
        partial.applyPicked(10, 4);
        partial.refreshStatusByShipped();
        assertThat(partial.getStatus()).isEqualTo(StoreOrderLineStatus.PARTIALLY_SHIPPED);

        StoreOrderLine full = line(10);
        full.increaseAllocated(10);
        full.applyPicked(10, 10);
        full.refreshStatusByShipped();
        assertThat(full.getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
    }

    @Test
    @DisplayName("부분 출고 후 나머지를 모두 출고하면 COMPLETED 로 바뀐다")
    void refreshStatusAfterRemaining() {
        StoreOrderLine l = line(10);
        l.increaseAllocated(10);
        l.applyPicked(10, 6);
        l.refreshStatusByShipped();
        l.increaseAllocated(4);
        l.applyPicked(4, 4);

        l.refreshStatusByShipped();

        assertThat(l.getStatus()).isEqualTo(StoreOrderLineStatus.COMPLETED);
    }

    @Test
    @DisplayName("취소된 항목은 상태를 다시 계산해도 바뀌지 않는다")
    void refreshStatusCanceled() {
        StoreOrderLine l = line(10);
        l.cancel();

        l.refreshStatusByShipped();

        assertThat(l.getStatus()).isEqualTo(StoreOrderLineStatus.CANCELED);
    }
}
