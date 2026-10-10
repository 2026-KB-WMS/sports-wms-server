package com.kb.wms.statushistory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import com.kb.wms.statushistory.adapter.out.persistence.repository.StatusHistoryJpaRepository;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.application.port.out.StatusHistoryRepository;
import com.kb.wms.statushistory.domain.entity.StatusHistory;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

/**
 * 상태 이력 영속성 어댑터와 기록 서비스의 트랜잭션 동작 검증.
 * 롤백·커밋을 직접 확인해야 하므로 테스트 자체는 트랜잭션으로 감싸지 않고 매번 직접 정리한다.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StatusHistoryPersistenceAdapterTest {

    private static final StatusHistoryEntityType ORDER = StatusHistoryEntityType.STORE_ORDER;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private StatusHistoryJpaRepository statusHistoryJpaRepository;

    @Autowired
    private StatusHistoryUseCase statusHistoryUseCase;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        statusHistoryJpaRepository.deleteAll();
    }

    private StatusHistory history(StatusHistoryEntityType type, Long entityId, String from, String to,
                                  String reason, LocalDateTime changedAt) {
        return StatusHistory.record(type, entityId, from, to, reason, 1L, changedAt);
    }

    @Test
    @DisplayName("저장하면 ID가 채워지고 최초 생성 이력의 fromStatus는 null로 읽힌다")
    void save_assignsIdAndKeepsNullFromStatus() {
        StatusHistory saved = statusHistoryRepository.save(
                history(ORDER, 1L, null, "REQUESTED", null, LocalDateTime.of(2026, 10, 3, 9, 0)));

        assertThat(saved.getStatusHistoryId()).isNotNull();
        assertThat(saved.getFromStatus()).isNull();
        assertThat(saved.isInitial()).isTrue();
    }

    @Test
    @DisplayName("엔티티별 이력은 시간순으로 조회되고 다른 엔티티·유형은 섞이지 않는다")
    void findByEntity_orderedAndIsolated() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 3, 9, 0);
        // 일부러 시간 역순으로 저장해 정렬을 검증한다.
        statusHistoryRepository.save(history(ORDER, 1L, "REQUESTED", "APPROVED", null, base.plusHours(1)));
        statusHistoryRepository.save(history(ORDER, 1L, null, "REQUESTED", null, base));
        statusHistoryRepository.save(history(ORDER, 1L, "APPROVED", "ASSIGNED", null, base.plusHours(2)));
        statusHistoryRepository.save(history(ORDER, 2L, null, "REQUESTED", null, base));
        statusHistoryRepository.save(history(StatusHistoryEntityType.INBOUND, 1L, null, "ARRIVED", null, base));

        List<StatusHistory> result = statusHistoryRepository.findByEntity(ORDER, 1L);

        assertThat(result).extracting(StatusHistory::getToStatus)
                .containsExactly("REQUESTED", "APPROVED", "ASSIGNED");
    }

    @Test
    @DisplayName("같은 시각에 기록된 이력은 저장 순서(ID)로 정렬된다")
    void findByEntity_sameTimestampOrderedById() {
        LocalDateTime at = LocalDateTime.of(2026, 10, 3, 9, 0);
        statusHistoryRepository.save(history(ORDER, 1L, null, "REQUESTED", null, at));
        statusHistoryRepository.save(history(ORDER, 1L, "REQUESTED", "APPROVED", null, at));

        assertThat(statusHistoryRepository.findByEntity(ORDER, 1L))
                .extracting(StatusHistory::getToStatus)
                .containsExactly("REQUESTED", "APPROVED");
    }

    @Test
    @DisplayName("이력이 없는 엔티티는 빈 목록을 돌려준다")
    void findByEntity_empty() {
        assertThat(statusHistoryRepository.findByEntity(ORDER, 999L)).isEmpty();
    }

    @Test
    @DisplayName("현재 상태로 가장 최근에 바뀐 이력의 사유를 조회한다")
    void findLatestByToStatus_returnsMostRecent() {
        LocalDateTime base = LocalDateTime.of(2026, 10, 3, 9, 0);
        // ON_HOLD가 두 번 있었다면 가장 최근 사유가 현재 사유다.
        statusHistoryRepository.save(history(ORDER, 1L, "ASSIGNED", "ON_HOLD", "재고 입고 대기", base));
        statusHistoryRepository.save(history(ORDER, 1L, "ON_HOLD", "ASSIGNED", null, base.plusHours(1)));
        statusHistoryRepository.save(history(ORDER, 1L, "ASSIGNED", "ON_HOLD", "물류 파업", base.plusHours(2)));

        assertThat(statusHistoryUseCase.findStatusReason(ORDER, 1L, "ON_HOLD")).contains("물류 파업");
    }

    @Test
    @DisplayName("해당 상태가 된 적이 없으면 빈 값을 돌려준다")
    void findLatestByToStatus_neverReached() {
        statusHistoryRepository.save(history(ORDER, 1L, null, "REQUESTED", null, LocalDateTime.now()));

        assertThat(statusHistoryRepository.findLatestByToStatus(ORDER, 1L, "REJECTED")).isEmpty();
        assertThat(statusHistoryUseCase.findStatusReason(ORDER, 1L, "REJECTED")).isEmpty();
    }

    @Test
    @DisplayName("호출한 트랜잭션이 커밋되면 이력도 함께 저장된다")
    void record_commitsWithCaller() {
        transactionTemplate.executeWithoutResult(status ->
                statusHistoryUseCase.record(ORDER, 1L, null, "REQUESTED", null, 1L));

        assertThat(statusHistoryRepository.findByEntity(ORDER, 1L)).hasSize(1);
    }

    @Test
    @DisplayName("호출한 트랜잭션이 롤백되면 이력도 함께 롤백된다")
    void record_rollsBackWithCaller() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            statusHistoryUseCase.record(ORDER, 1L, null, "REQUESTED", null, 1L);
            throw new IllegalStateException("도메인 서비스 실패");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(statusHistoryRepository.findByEntity(ORDER, 1L)).isEmpty();
    }

    @Test
    @DisplayName("진행 중인 트랜잭션 없이 기록하면 단독 커밋되지 않고 예외를 던진다")
    void record_withoutTransaction_throws() {
        assertThatThrownBy(() -> statusHistoryUseCase.record(ORDER, 1L, null, "REQUESTED", null, 1L))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(statusHistoryRepository.findByEntity(ORDER, 1L)).isEmpty();
    }
}
