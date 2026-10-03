package com.kb.wms.common.statushistory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.statushistory.application.port.out.StatusHistoryRepository;
import com.kb.wms.common.statushistory.domain.entity.StatusHistory;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

@ExtendWith(MockitoExtension.class)
class StatusHistoryServiceTest {

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    private StatusHistoryService statusHistoryService;

    @Test
    @DisplayName("이력을 기록하면 이전·이후 상태, 사유, 처리자와 변경 시각이 저장된다")
    void record_savesHistory() {
        when(statusHistoryRepository.save(any(StatusHistory.class))).thenAnswer(i -> i.getArgument(0));

        statusHistoryService.record(
                StatusHistoryEntityType.STORE_ORDER, 5L, "REQUESTED", "REJECTED", "재고 부족", 7L);

        ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        StatusHistory saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(StatusHistoryEntityType.STORE_ORDER);
        assertThat(saved.getEntityId()).isEqualTo(5L);
        assertThat(saved.getFromStatus()).isEqualTo("REQUESTED");
        assertThat(saved.getToStatus()).isEqualTo("REJECTED");
        assertThat(saved.getReason()).isEqualTo("재고 부족");
        assertThat(saved.getChangedBy()).isEqualTo(7L);
        assertThat(saved.getChangedAt()).isNotNull().isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("이력 조회는 저장소 결과를 그대로 돌려준다")
    void findHistory_delegates() {
        StatusHistory first = StatusHistory.record(
                StatusHistoryEntityType.INBOUND, 1L, null, "ARRIVED", null, 1L, LocalDateTime.now());
        when(statusHistoryRepository.findByEntity(StatusHistoryEntityType.INBOUND, 1L))
                .thenReturn(List.of(first));

        assertThat(statusHistoryService.findHistory(StatusHistoryEntityType.INBOUND, 1L)).containsExactly(first);
    }

    @Test
    @DisplayName("현재 상태로 바뀔 때 기록된 사유를 돌려준다")
    void findStatusReason_returnsReason() {
        StatusHistory rejected = StatusHistory.record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "REJECTED", "재고 부족", 1L, LocalDateTime.now());
        when(statusHistoryRepository.findLatestByToStatus(StatusHistoryEntityType.STORE_ORDER, 1L, "REJECTED"))
                .thenReturn(Optional.of(rejected));

        assertThat(statusHistoryService.findStatusReason(StatusHistoryEntityType.STORE_ORDER, 1L, "REJECTED"))
                .contains("재고 부족");
    }

    @Test
    @DisplayName("이력이 없거나 사유가 없으면 빈 값을 돌려준다")
    void findStatusReason_empty() {
        StatusHistory noReason = StatusHistory.record(
                StatusHistoryEntityType.STORE_ORDER, 1L, "REQUESTED", "APPROVED", null, 1L, LocalDateTime.now());
        when(statusHistoryRepository.findLatestByToStatus(StatusHistoryEntityType.STORE_ORDER, 1L, "APPROVED"))
                .thenReturn(Optional.of(noReason));
        when(statusHistoryRepository.findLatestByToStatus(StatusHistoryEntityType.STORE_ORDER, 1L, "CANCELED"))
                .thenReturn(Optional.empty());

        assertThat(statusHistoryService.findStatusReason(StatusHistoryEntityType.STORE_ORDER, 1L, "APPROVED"))
                .isEmpty();
        assertThat(statusHistoryService.findStatusReason(StatusHistoryEntityType.STORE_ORDER, 1L, "CANCELED"))
                .isEmpty();
    }
}
