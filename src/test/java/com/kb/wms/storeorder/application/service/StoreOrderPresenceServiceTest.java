package com.kb.wms.storeorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

@ExtendWith(MockitoExtension.class)
class StoreOrderPresenceServiceTest {

    @Mock
    private StoreOrderRepository storeOrderRepository;

    @InjectMocks
    private StoreOrderPresenceService presenceService;

    @Test
    @DisplayName("진행 중 상태(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)만 확인 대상으로 조회한다")
    @SuppressWarnings("unchecked")
    void hasInProgressOrders_checksOnlyInProgressStatuses() {
        when(storeOrderRepository.existsByStoreIdAndStatusIn(eq(2L), any())).thenReturn(true);

        assertThat(presenceService.hasInProgressOrders(2L)).isTrue();

        ArgumentCaptor<Collection<StoreOrderStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(storeOrderRepository).existsByStoreIdAndStatusIn(eq(2L), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
                StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD);
    }

    @Test
    @DisplayName("진행 중인 발주가 없으면 false")
    void hasInProgressOrders_none() {
        when(storeOrderRepository.existsByStoreIdAndStatusIn(eq(2L), any())).thenReturn(false);

        assertThat(presenceService.hasInProgressOrders(2L)).isFalse();
    }
}
