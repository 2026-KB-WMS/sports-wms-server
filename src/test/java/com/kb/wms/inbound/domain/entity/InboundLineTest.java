package com.kb.wms.inbound.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InboundLineTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 2, 11, 0);

    private static InboundLine line(long received, long accepted, long defective, BigDecimal price,
                                    Long acceptedSectionId, Long defectSectionId) {
        return InboundLine.register(7L, 11L, 1L, acceptedSectionId, defectSectionId,
                received, accepted, defective, price, null, "비고", NOW, 5L);
    }

    @Test
    @DisplayName("항목 금액은 입고 수량 × 입고 단가로 계산한다")
    void register_calculatesLineAmount() {
        InboundLine line = line(60, 58, 2, BigDecimal.valueOf(60000), 2L, 9L);

        assertThat(line.getLineAmount()).isEqualByComparingTo("3600000");
        assertThat(line.getReceivedQuantity()).isEqualTo(60L);
        assertThat(line.getAcceptedQuantity()).isEqualTo(58L);
        assertThat(line.getDefectiveQuantity()).isEqualTo(2L);
        assertThat(line.getReceivedBy()).isEqualTo(5L);
        assertThat(line.getReceivedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("입고 수량이 0 이하이면 거절한다")
    void register_rejectsNonPositiveReceivedQuantity() {
        assertThatThrownBy(() -> line(0, 0, 0, BigDecimal.TEN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("합격·불량 수량이 음수이면 거절한다")
    void register_rejectsNegativeQuantities() {
        assertThatThrownBy(() -> line(10, -1, 11, BigDecimal.TEN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line(10, 11, -1, BigDecimal.TEN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("합격 + 불량이 입고 수량과 다르면 거절한다")
    void register_rejectsQuantityMismatch() {
        assertThatThrownBy(() -> line(10, 5, 4, BigDecimal.TEN, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("입고 단가가 없거나 음수이면 거절한다")
    void register_rejectsInvalidPrice() {
        assertThatThrownBy(() -> line(10, 10, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> line(10, 10, 0, BigDecimal.valueOf(-1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("입고 단가가 0이어도 등록할 수 있다")
    void register_allowsZeroPrice() {
        assertThat(line(10, 10, 0, BigDecimal.ZERO, 2L, null).getLineAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("합격 수량이 있으면 합격 구역이, 불량 수량이 있으면 불량 구역이 있어야 완료할 수 있다")
    void hasRequiredSections() {
        assertThat(line(60, 58, 2, BigDecimal.TEN, 2L, 9L).hasRequiredSections()).isTrue();
        assertThat(line(60, 58, 2, BigDecimal.TEN, null, 9L).hasRequiredSections()).isFalse();
        assertThat(line(60, 58, 2, BigDecimal.TEN, 2L, null).hasRequiredSections()).isFalse();
        // 불량이 없으면 불량 구역이 없어도, 합격이 없으면 합격 구역이 없어도 된다
        assertThat(line(60, 60, 0, BigDecimal.TEN, 2L, null).hasRequiredSections()).isTrue();
        assertThat(line(2, 0, 2, BigDecimal.TEN, null, 9L).hasRequiredSections()).isTrue();
    }
}
