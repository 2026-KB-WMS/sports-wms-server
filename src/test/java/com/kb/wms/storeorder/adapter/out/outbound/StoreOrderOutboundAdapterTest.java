package com.kb.wms.storeorder.adapter.out.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.outbound.application.port.in.OutboundOrderLinkUseCase;
import com.kb.wms.outbound.application.port.in.result.OrderFulfillmentCancelResult;
import com.kb.wms.outbound.application.port.in.result.OrderOutboundSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderOutboundView;
import com.kb.wms.storeorder.domain.enums.StoreOrderOutboundStatus;

@ExtendWith(MockitoExtension.class)
class StoreOrderOutboundAdapterTest {

    @Mock OutboundOrderLinkUseCase outboundOrderLinkUseCase;
    @InjectMocks StoreOrderOutboundAdapter adapter;

    @Test
    @DisplayName("출고가 내보낸 상태 이름을 발주의 출고 상태 enum으로 바꾼다")
    void mapsStatusNames() {
        when(outboundOrderLinkUseCase.findLatestOutboundStatus(1L)).thenReturn(Optional.of("PICKING"));
        when(outboundOrderLinkUseCase.findLatestOutboundStatuses(Set.of(1L, 2L)))
                .thenReturn(Map.of(1L, "CANCELED", 2L, "DELIVERED"));

        assertThat(adapter.findLatestOutboundStatus(1L)).contains(StoreOrderOutboundStatus.PICKING);
        assertThat(adapter.findLatestOutboundStatuses(Set.of(1L, 2L))).containsExactlyInAnyOrderEntriesOf(
                Map.of(1L, StoreOrderOutboundStatus.CANCELED, 2L, StoreOrderOutboundStatus.DELIVERED));
    }

    @Test
    @DisplayName("모르는 출고 상태 이름은 거절한다")
    void rejectsUnknownStatus() {
        when(outboundOrderLinkUseCase.findLatestOutboundStatus(1L)).thenReturn(Optional.of("UNKNOWN"));

        assertThatThrownBy(() -> adapter.findLatestOutboundStatus(1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("출고 요약과 취소 결과를 발주의 타입으로 바꾼다")
    void mapsViewsAndResult() {
        LocalDateTime shippedAt = LocalDateTime.of(2026, 10, 5, 9, 0);
        when(outboundOrderLinkUseCase.findOutbounds(1L)).thenReturn(List.of(
                new OrderOutboundSummary(7L, "OB-7", "SHIPPED", shippedAt, null)));
        when(outboundOrderLinkUseCase.cancelFulfillment(1L, 9L)).thenReturn(new OrderFulfillmentCancelResult(3, 2));

        List<StoreOrderOutboundView> views = adapter.findOutbounds(1L);
        StoreOrderFulfillmentCancelResult result = adapter.cancelFulfillment(1L, 9L);

        assertThat(views).singleElement().satisfies(v -> {
            assertThat(v.outboundId()).isEqualTo(7L);
            assertThat(v.status()).isEqualTo("SHIPPED");
            assertThat(v.shippedAt()).isEqualTo(shippedAt);
        });
        assertThat(result.releasedAllocationCount()).isEqualTo(3);
        assertThat(result.canceledOutboundCount()).isEqualTo(2);
    }
}
