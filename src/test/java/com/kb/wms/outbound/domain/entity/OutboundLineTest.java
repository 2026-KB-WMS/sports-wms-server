package com.kb.wms.outbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutboundLineTest {

    private OutboundLine newLine() {
        return OutboundLine.create(1L, 2L);
    }

    @Test
    @DisplayName("생성 직후에는 수량 0, 단가/금액 없음")
    void create() {
        OutboundLine line = newLine();

        assertThat(line.getShippedQuantity()).isZero();
        assertThat(line.getConfirmedUnitSupplyPrice()).isNull();
        assertThat(line.isPickingConfirmed()).isFalse();
        assertThat(line.lineAmount()).isNull();
    }

    @Test
    @DisplayName("할당이 없으면 생성 실패")
    void createInvalid() {
        assertThatThrownBy(() -> OutboundLine.create(1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("피킹 확정 시 수량/단가가 기록되고 금액이 계산된다")
    void confirmPicking() {
        OutboundLine line = newLine();

        line.confirmPicking(7, 10, new BigDecimal("1500.00"));

        assertThat(line.getShippedQuantity()).isEqualTo(7);
        assertThat(line.isPickingConfirmed()).isTrue();
        assertThat(line.lineAmount()).isEqualByComparingTo("10500.00");
    }

    @Test
    @DisplayName("피킹 수량 0 이면 금액도 0")
    void confirmPickingZero() {
        OutboundLine line = newLine();

        line.confirmPicking(0, 10, new BigDecimal("1500.00"));

        assertThat(line.lineAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("피킹 확정은 한 번만 가능")
    void confirmTwice() {
        OutboundLine line = newLine();
        line.confirmPicking(5, 10, BigDecimal.TEN);

        assertThatThrownBy(() -> line.confirmPicking(5, 10, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("수량 범위/단가 오류는 예외이며 상태가 바뀌지 않는다")
    void confirmInvalid() {
        OutboundLine line = newLine();

        assertThatThrownBy(() -> line.confirmPicking(-1, 10, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line.confirmPicking(11, 10, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line.confirmPicking(5, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line.confirmPicking(5, 10, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(line.isPickingConfirmed()).isFalse();
    }
}
