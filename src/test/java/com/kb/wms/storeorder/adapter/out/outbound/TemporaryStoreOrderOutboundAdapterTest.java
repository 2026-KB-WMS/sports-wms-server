package com.kb.wms.storeorder.adapter.out.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.kb.wms.storeorder.application.port.in.result.StoreOrderFulfillmentCancelResult;

class TemporaryStoreOrderOutboundAdapterTest {

    private final TemporaryStoreOrderOutboundAdapter adapter = new TemporaryStoreOrderOutboundAdapter();

    @Test
    @DisplayName("출고가 없다고 답해 최근 출고 상태와 출고 목록이 비어 있다")
    void noOutbounds() {
        assertThat(adapter.findLatestOutboundStatus(1L)).isEmpty();
        assertThat(adapter.findLatestOutboundStatuses(List.of(1L, 2L))).isEmpty();
        assertThat(adapter.findOutbounds(1L)).isEmpty();
    }

    @Test
    @DisplayName("진행 중 출고·피킹 시작·할당 검사는 모두 통과(없음)한다")
    void guardsPass() {
        assertThat(adapter.existsPickingStarted(1L)).isFalse();
        assertThat(adapter.existsInProgressOutbound(1L)).isFalse();
        assertThat(adapter.existsActiveFulfillment(1L)).isFalse();
    }

    @Test
    @DisplayName("취소 때 정리한 할당·출고는 0건이다")
    void cancelFulfillment_none() {
        StoreOrderFulfillmentCancelResult result = adapter.cancelFulfillment(1L, 9L);

        assertThat(result.releasedAllocationCount()).isZero();
        assertThat(result.canceledOutboundCount()).isZero();
    }
}
