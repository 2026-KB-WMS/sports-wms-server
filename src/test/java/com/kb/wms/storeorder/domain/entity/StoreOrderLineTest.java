package com.kb.wms.storeorder.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.kb.wms.storeorder.domain.enums.StoreOrderLineStatus;

class StoreOrderLineTest {

    private StoreOrderLine line(Long skuId, long quantity, String price) {
        return StoreOrderLine.register(1L, skuId, quantity, new BigDecimal(price));
    }

    private StoreOrderLine withStatus(StoreOrderLineStatus status) {
        return StoreOrderLine.builder()
                .storeOrderId(1L).skuId(10L).requestedQuantity(10L)
                .requestedUnitSupplyPrice(new BigDecimal("1000"))
                .status(status)
                .build();
    }

    @Test
    @DisplayName("등록하면 REQUESTED이고 할당·출고 수량은 0이다")
    void register_initialState() {
        StoreOrderLine line = line(10L, 5, "1500.00");

        assertThat(line.getStatus()).isEqualTo(StoreOrderLineStatus.REQUESTED);
        assertThat(line.getRequestedQuantity()).isEqualTo(5L);
        assertThat(line.getAllocatedQuantity()).isZero();
        assertThat(line.getShippedQuantity()).isZero();
        assertThat(line.getRequestedUnitSupplyPrice()).isEqualByComparingTo("1500.00");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {0, -1})
    @DisplayName("요청 수량이 1 미만이면 예외가 발생한다")
    void register_invalidQuantity_throws(long quantity) {
        assertThatThrownBy(() -> line(10L, quantity, "1000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공급 단가가 없거나 음수이면 예외가 발생한다")
    void register_invalidPrice_throws() {
        assertThatThrownBy(() -> StoreOrderLine.register(1L, 10L, 5, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line(10L, 5, "-0.01"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("공급 단가가 0이어도 등록할 수 있다")
    void register_zeroPrice_allowed() {
        assertThat(line(10L, 5, "0").lineAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("항목 금액은 요청 수량 × 공급 단가다")
    void lineAmount() {
        assertThat(line(10L, 3, "1250.50").lineAmount()).isEqualByComparingTo("3751.50");
    }

    @Test
    @DisplayName("발주 총액은 항목 금액의 합계이고 항목이 없으면 0이다")
    void totalAmount() {
        List<StoreOrderLine> lines = List.of(line(10L, 3, "1000"), line(11L, 2, "2500.50"));

        assertThat(StoreOrderLine.totalAmount(lines)).isEqualByComparingTo("8001.00");
        assertThat(StoreOrderLine.totalAmount(List.of())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("같은 SKU가 두 줄이면 예외가 발생하고 서로 다른 SKU면 통과한다")
    void requireDistinctSkus() {
        StoreOrderLine.requireDistinctSkus(List.of(line(10L, 1, "1000"), line(11L, 1, "1000")));

        assertThatThrownBy(() -> StoreOrderLine.requireDistinctSkus(
                List.of(line(10L, 1, "1000"), line(10L, 2, "1000"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("REQUESTED 항목을 취소하면 CANCELED가 된다")
    void cancel_fromRequested() {
        StoreOrderLine line = withStatus(StoreOrderLineStatus.REQUESTED);

        line.cancel();

        assertThat(line.getStatus()).isEqualTo(StoreOrderLineStatus.CANCELED);
    }

    @ParameterizedTest
    @EnumSource(value = StoreOrderLineStatus.class, names = {"PARTIALLY_SHIPPED", "COMPLETED", "CANCELED"})
    @DisplayName("출고가 시작되었거나 이미 종결된 항목은 취소할 수 없다")
    void cancel_fromOtherStatus_throws(StoreOrderLineStatus status) {
        StoreOrderLine line = withStatus(status);

        assertThatThrownBy(line::cancel).isInstanceOf(IllegalStateException.class);
        assertThat(line.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("부족 수량은 요청 수량에서 출고 수량을 뺀 값이고 초과 출고여도 음수가 되지 않는다")
    void remainingQuantity() {
        StoreOrderLine none = withStatus(StoreOrderLineStatus.REQUESTED);
        StoreOrderLine partial = StoreOrderLine.builder()
                .requestedQuantity(10L).shippedQuantity(4L)
                .requestedUnitSupplyPrice(BigDecimal.ONE).build();
        StoreOrderLine over = StoreOrderLine.builder()
                .requestedQuantity(10L).shippedQuantity(12L)
                .requestedUnitSupplyPrice(BigDecimal.ONE).build();

        assertThat(none.remainingQuantity()).isEqualTo(10L);
        assertThat(partial.remainingQuantity()).isEqualTo(6L);
        assertThat(over.remainingQuantity()).isZero();
    }

    @Test
    @DisplayName("출고 수량이 요청 수량 이상일 때만 전량 출고로 본다")
    void isFulfilled() {
        StoreOrderLine partial = StoreOrderLine.builder()
                .requestedQuantity(10L).shippedQuantity(9L)
                .requestedUnitSupplyPrice(BigDecimal.ONE).build();
        StoreOrderLine exact = StoreOrderLine.builder()
                .requestedQuantity(10L).shippedQuantity(10L)
                .requestedUnitSupplyPrice(BigDecimal.ONE).build();

        assertThat(partial.isFulfilled()).isFalse();
        assertThat(exact.isFulfilled()).isTrue();
    }
}
