package com.kb.wms.store.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.command.StoreRegisterCommand;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.store.exception.StoreErrorCode;
import com.kb.wms.storeorder.application.port.out.StoreOrderRepository;
import com.kb.wms.storeorder.domain.entity.StoreOrder;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

/**
 * 지점 비활성화와 지점 발주 상태의 통합 검증(실제 빈 연결: 지점 → 발주 조회 유스케이스).
 * 진행 중 상태(REQUESTED·APPROVED·ASSIGNED·ON_HOLD)의 발주가 있으면 막고, 종결 상태만 있으면 허용한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StoreDeactivationWithOrdersTest {

    @Autowired StoreUseCase storeUseCase;
    @Autowired StoreOrderRepository storeOrderRepository;

    @Test
    @DisplayName("진행 중인 발주가 있으면 지점을 비활성화할 수 없다(STORE_IN_USE)")
    void inProgressOrder_blocksDeactivation() {
        for (StoreOrderStatus status : new StoreOrderStatus[] {
                StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
                StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD}) {
            Store store = newStore("ST-INUSE-" + status);
            order("SO-INUSE-" + status, store.getStoreId(), status);

            assertThatThrownBy(() -> storeUseCase.deactivateStore(store.getStoreId()))
                    .as(status.name())
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCodeName())
                    .isEqualTo(StoreErrorCode.STORE_IN_USE.name());
        }
    }

    @Test
    @DisplayName("종결된 발주(완료·취소·반려)만 있으면 지점을 비활성화할 수 있다")
    void closedOrdersOnly_allowDeactivation() {
        Store store = newStore("ST-CLOSED");
        order("SO-CLOSED-1", store.getStoreId(), StoreOrderStatus.COMPLETED);
        order("SO-CLOSED-2", store.getStoreId(), StoreOrderStatus.CANCELED);
        order("SO-CLOSED-3", store.getStoreId(), StoreOrderStatus.REJECTED);

        Store result = storeUseCase.deactivateStore(store.getStoreId());

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("다른 지점의 진행 중 발주는 영향을 주지 않는다")
    void otherStoreOrder_doesNotBlock() {
        Store target = newStore("ST-TARGET");
        Store other = newStore("ST-OTHER");
        order("SO-OTHER-1", other.getStoreId(), StoreOrderStatus.REQUESTED);

        assertThat(storeUseCase.deactivateStore(target.getStoreId()).isActive()).isFalse();
    }

    private Store newStore(String code) {
        return storeUseCase.registerStore(new StoreRegisterCommand(code, code, "주소", "담당자", "02-1234-5678"));
    }

    private void order(String orderNo, Long storeId, StoreOrderStatus status) {
        storeOrderRepository.save(StoreOrder.builder()
                .orderNo(orderNo).storeId(storeId).status(status)
                .requestedAt(LocalDateTime.of(2026, 10, 1, 9, 0)).createdBy(1L)
                .build());
    }
}
