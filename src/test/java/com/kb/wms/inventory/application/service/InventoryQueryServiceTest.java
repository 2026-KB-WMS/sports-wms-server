package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LowStockSearchCondition;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    @Mock
    private InventoryQueryRepository inventoryQueryRepository;
    @Mock
    private InventoryLotRepository inventoryLotRepository;
    @Mock
    private LotRepository lotRepository;

    @InjectMocks
    private InventoryQueryService inventoryQueryService;

    @Test
    @DisplayName("skuId·warehouseId 필터가 모두 존재하면 SKU 요약 목록을 조회한다")
    void getInventories_success() {
        InventorySearchCondition condition = new InventorySearchCondition(1L, 2L, null);
        when(inventoryQueryRepository.existsSku(1L)).thenReturn(true);
        when(inventoryQueryRepository.existsWarehouse(2L)).thenReturn(true);
        List<InventorySkuSummary> expected = List.of();
        when(inventoryQueryRepository.findSkuSummaries(condition)).thenReturn(expected);

        List<InventorySkuSummary> result = inventoryQueryService.getInventories(condition);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 skuId로 필터링하면 NOT_FOUND 예외를 던진다")
    void getInventories_skuNotFound() {
        InventorySearchCondition condition = new InventorySearchCondition(999L, null, null);
        when(inventoryQueryRepository.existsSku(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventories(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findSkuSummaries(condition);
    }

    @Test
    @DisplayName("존재하지 않는 warehouseId로 필터링하면 NOT_FOUND 예외를 던진다")
    void getInventories_warehouseNotFound() {
        InventorySearchCondition condition = new InventorySearchCondition(null, 999L, null);
        when(inventoryQueryRepository.existsWarehouse(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventories(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("필터 없이 조회하면 존재 검증 없이 목록을 조회한다")
    void getInventories_noFilter_skipsValidation() {
        InventorySearchCondition condition = new InventorySearchCondition(null, null, null);
        when(inventoryQueryRepository.findSkuSummaries(condition)).thenReturn(List.of());

        inventoryQueryService.getInventories(condition);

        verify(inventoryQueryRepository, never()).existsSku(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 sectionId로 로트별 재고를 조회하면 NOT_FOUND 예외를 던진다")
    void getInventoriesByLot_sectionNotFound() {
        InventoryLotSearchCondition condition =
                new InventoryLotSearchCondition(null, null, 999L, null, null, null, null);
        when(inventoryQueryRepository.existsSection(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventoriesByLot(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하는 재고를 조회하면 상세를 반환한다")
    void getInventory_success() {
        InventoryDetail detail = new InventoryDetail(1L, 1L, "서울센터", 1L, "A-01", "1구역", 1L, "SKU-001", "상품",
                "EA", 1L, "LOT-001", null, null, null, null, null, 100L, 20L, 80L, null, null, null, null);
        when(inventoryQueryRepository.findDetail(1L)).thenReturn(Optional.of(detail));

        InventoryDetail result = inventoryQueryService.getInventory(1L);

        assertThat(result).isSameAs(detail);
    }

    @Test
    @DisplayName("존재하지 않는 재고를 조회하면 NOT_FOUND 예외를 던진다")
    void getInventory_notFound() {
        when(inventoryQueryRepository.findDetail(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryQueryService.getInventory(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 warehouseId로 저재고를 조회하면 NOT_FOUND 예외를 던진다")
    void getLowStock_warehouseNotFound() {
        LowStockSearchCondition condition = new LowStockSearchCondition(999L, null);
        when(inventoryQueryRepository.existsWarehouse(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getLowStock(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("createdFrom이 createdTo보다 이후이면 VALIDATION_ERROR 예외를 던진다")
    void getTransactions_invalidPeriod() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null,
                LocalDateTime.of(2026, 1, 10, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("referenceId만 지정하고 referenceType을 지정하지 않으면 VALIDATION_ERROR 예외를 던진다")
    void getTransactions_referenceIdWithoutType() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, 5L, null, null);

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("존재하지 않는 lotId로 이력을 조회하면 NOT_FOUND 예외를 던진다")
    void getTransactions_lotNotFound() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, 999L, null, null, null, null, null);
        when(lotRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 재고의 이력을 조회하면 NOT_FOUND 예외를 던진다")
    void getTransactionsOf_notFound() {
        when(inventoryLotRepository.existsById(999L)).thenReturn(false);
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                999L, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> inventoryQueryService.getTransactionsOf(999L, condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
    }
}
