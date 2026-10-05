package com.kb.wms.store.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;
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
    @Autowired StatusHistoryUseCase statusHistoryUseCase;

    @Test
    @DisplayName("진행 중인 발주가 있으면 지점을 비활성화할 수 없다(STORE_IN_USE)")
    void inProgressOrder_blocksDeactivation() {
        for (StoreOrderStatus status : new StoreOrderStatus[] {
                StoreOrderStatus.REQUESTED, StoreOrderStatus.APPROVED,
                StoreOrderStatus.ASSIGNED, StoreOrderStatus.ON_HOLD}) {
            Store store = newStore("ST-INUSE-" + status);
            order("SO-INUSE-" + status, store.getStoreId(), status);

            assertThatThrownBy(() -> storeUseCase.deactivateStore(store.getStoreId(), null, 1L))
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

        Store result = storeUseCase.deactivateStore(store.getStoreId(), null, 1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("다른 지점의 진행 중 발주는 영향을 주지 않는다")
    void otherStoreOrder_doesNotBlock() {
        Store target = newStore("ST-TARGET");
        Store other = newStore("ST-OTHER");
        order("SO-OTHER-1", other.getStoreId(), StoreOrderStatus.REQUESTED);

        assertThat(storeUseCase.deactivateStore(target.getStoreId(), null, 1L).isActive()).isFalse();
    }

    @Test
    @DisplayName("비활성화 사유는 StatusHistory(STORE)에 ACTIVE→INACTIVE로 저장된다")
    void deactivate_recordsReasonInStatusHistory() {
        Store store = newStore("ST-REASON");

        storeUseCase.deactivateStore(store.getStoreId(), "  폐점 처리  ", 7L);

        List<StatusHistory> history = statusHistoryUseCase.findHistory(StatusHistoryEntityType.STORE, store.getStoreId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFromStatus()).isEqualTo("ACTIVE");
        assertThat(history.get(0).getToStatus()).isEqualTo("INACTIVE");
        assertThat(history.get(0).getReason()).isEqualTo("폐점 처리");
        assertThat(history.get(0).getChangedBy()).isEqualTo(7L);
    }

    @Test
    @DisplayName("사유 없이 비활성화해도 이력은 reason null로 남는다")
    void deactivate_withoutReason_recordsNullReason() {
        Store store = newStore("ST-NOREASON");

        storeUseCase.deactivateStore(store.getStoreId(), null, 7L);

        List<StatusHistory> history = statusHistoryUseCase.findHistory(StatusHistoryEntityType.STORE, store.getStoreId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getReason()).isNull();
    }

    @Test
    @DisplayName("사유가 500자를 넘으면 VALIDATION_ERROR이고 비활성화되지 않는다")
    void deactivate_reasonTooLong_rejected() {
        Store store = newStore("ST-LONG");

        assertThatThrownBy(() -> storeUseCase.deactivateStore(store.getStoreId(), "가".repeat(501), 7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThat(storeUseCase.getStore(store.getStoreId()).isActive()).isTrue();
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
