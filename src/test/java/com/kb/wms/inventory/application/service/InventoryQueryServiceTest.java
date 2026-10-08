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

import com.kb.wms.inventory.domain.enums.QualityStatus;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
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
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

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

        List<InventorySkuSummary> result = inventoryQueryService.getInventories(condition, HQ);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 skuId로 필터링하면 SKU_NOT_FOUND 예외를 던진다")
    void getInventories_skuNotFound() {
        InventorySearchCondition condition = new InventorySearchCondition(999L, null, null);
        when(inventoryQueryRepository.existsSku(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventories(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.SKU_NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findSkuSummaries(condition);
    }

    @Test
    @DisplayName("존재하지 않는 warehouseId로 필터링하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void getInventories_warehouseNotFound() {
        InventorySearchCondition condition = new InventorySearchCondition(null, 999L, null);
        when(inventoryQueryRepository.existsWarehouse(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventories(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("필터 없이 조회하면 존재 검증 없이 목록을 조회한다")
    void getInventories_noFilter_skipsValidation() {
        InventorySearchCondition condition = new InventorySearchCondition(null, null, null);
        when(inventoryQueryRepository.findSkuSummaries(condition)).thenReturn(List.of());

        inventoryQueryService.getInventories(condition, HQ);

        verify(inventoryQueryRepository, never()).existsSku(anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 sectionId로 로트별 재고를 조회하면 SECTION_NOT_FOUND 예외를 던진다")
    void getInventoriesByLot_sectionNotFound() {
        InventoryLotSearchCondition condition =
                new InventoryLotSearchCondition(null, null, 999L, null, null, null, null);
        when(inventoryQueryRepository.existsSection(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getInventoriesByLot(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.SECTION_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하는 재고를 조회하면 상세를 반환한다")
    void getInventory_success() {
        InventoryDetail detail = new InventoryDetail(1L, 1L, "서울센터", 1L, "A-01", "1구역", 1L, "SKU-001", "상품",
                "EA", 1L, "LOT-001", null, "한빛식품", null, null, null, null, 100L, 20L, 80L, null, null, null, null);
        when(inventoryQueryRepository.findDetail(1L)).thenReturn(Optional.of(detail));

        InventoryDetail result = inventoryQueryService.getInventory(1L, HQ);

        assertThat(result).isSameAs(detail);
    }

    @Test
    @DisplayName("존재하지 않는 재고를 조회하면 INVENTORY_NOT_FOUND 예외를 던진다")
    void getInventory_notFound() {
        when(inventoryQueryRepository.findDetail(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryQueryService.getInventory(999L, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 warehouseId로 저재고를 조회하면 WAREHOUSE_NOT_FOUND 예외를 던진다")
    void getLowStock_warehouseNotFound() {
        LowStockSearchCondition condition = new LowStockSearchCondition(999L, null);
        when(inventoryQueryRepository.existsWarehouse(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getLowStock(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.WAREHOUSE_NOT_FOUND.name());
    }

    @Test
    @DisplayName("createdFrom이 createdTo보다 이후이면 VALIDATION_ERROR 예외를 던진다")
    void getTransactions_invalidPeriod() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null,
                LocalDateTime.of(2026, 1, 10, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("referenceId만 지정하고 referenceType을 지정하지 않으면 VALIDATION_ERROR 예외를 던진다")
    void getTransactions_referenceIdWithoutType() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, 5L, null, null);

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("존재하지 않는 lotId로 이력을 조회하면 LOT_NOT_FOUND 예외를 던진다")
    void getTransactions_lotNotFound() {
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                null, null, null, null, 999L, null, null, null, null, null);
        when(lotRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryQueryService.getTransactions(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 재고의 이력을 조회하면 INVENTORY_NOT_FOUND 예외를 던진다")
    void getTransactionsOf_notFound() {
        when(inventoryQueryRepository.findDetail(999L)).thenReturn(Optional.empty());
        InventoryTransactionSearchCondition condition = new InventoryTransactionSearchCondition(
                999L, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> inventoryQueryService.getTransactionsOf(999L, condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.INVENTORY_NOT_FOUND.name());
    }


    private final AuthenticatedUser manager =
            new AuthenticatedUser(2L, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of());

    private static InventoryDetail detailIn(Long warehouseId) {
        return new InventoryDetail(1L, warehouseId, "서울센터", 1L, "A-01", "1구역", 1L, "SKU-001", "상품A",
                "EA", 1L, "LOT-001", 3L, "한빛식품", null, null, null, null, 100L, 20L, 80L, QualityStatus.AVAILABLE,
                null, null, null);
    }

    @Test
    @DisplayName("창고 관리자가 창고를 생략하면 담당 창고 목록으로 좁혀 조회한다 (재고·로트별·안전재고·이력)")
    void list_warehouseManagerWithoutWarehouse_scopedToAssigned() {
        List<Long> scope = List.of(1L, 2L);
        when(inventoryQueryRepository.findSkuSummaries(new InventorySearchCondition(null, null, null, scope)))
                .thenReturn(List.of());
        when(inventoryQueryRepository.findLowStock(new LowStockSearchCondition(null, null, scope)))
                .thenReturn(List.of());
        when(inventoryQueryRepository.findLotViews(new InventoryLotSearchCondition(
                null, null, null, null, null, null, null, scope))).thenReturn(List.of());
        when(inventoryQueryRepository.findTransactions(new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null, null, null, scope))).thenReturn(List.of());

        inventoryQueryService.getInventories(new InventorySearchCondition(null, null, null), manager);
        inventoryQueryService.getLowStock(new LowStockSearchCondition(null, null), manager);
        inventoryQueryService.getInventoriesByLot(
                new InventoryLotSearchCondition(null, null, null, null, null, null, null), manager);
        inventoryQueryService.getTransactions(new InventoryTransactionSearchCondition(
                null, null, null, null, null, null, null, null, null, null), manager);

        verify(inventoryQueryRepository).findSkuSummaries(new InventorySearchCondition(null, null, null, scope));
    }

    @Test
    @DisplayName("지정한 담당 창고는 추가 범위 없이 그 창고로만 조회하고, 비담당 창고면 존재 확인 전에 403이다")
    void list_specifiedWarehouse() {
        when(inventoryQueryRepository.existsWarehouse(2L)).thenReturn(true);
        when(inventoryQueryRepository.findSkuSummaries(new InventorySearchCondition(null, 2L, null, null)))
                .thenReturn(List.of());

        inventoryQueryService.getInventories(new InventorySearchCondition(null, 2L, null), manager);

        assertThatThrownBy(() -> inventoryQueryService.getInventories(new InventorySearchCondition(null, 3L, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> inventoryQueryService.getLowStock(new LowStockSearchCondition(3L, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(inventoryQueryRepository, never()).existsWarehouse(3L);
    }

    @Test
    @DisplayName("구역을 지정하면 그 구역의 창고가 담당 창고여야 한다")
    void list_sectionOfOtherWarehouse_forbidden() {
        when(inventoryQueryRepository.findWarehouseIdOfSection(7L)).thenReturn(Optional.of(3L));

        assertThatThrownBy(() -> inventoryQueryService.getInventoriesByLot(new InventoryLotSearchCondition(null, null, 7L, null, null, null, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> inventoryQueryService.getTransactions(new InventoryTransactionSearchCondition(null, null, 7L, null, null, null, null, null, null, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(inventoryQueryRepository, never()).findLotViews(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("재고 상세·재고별 이력은 재고가 담당 창고에 있을 때만 보이고, 아니면 403이며 이력을 조회하지 않는다")
    void detailAndTransactionsOf_otherWarehouse_forbidden() {
        when(inventoryQueryRepository.findDetail(1L)).thenReturn(Optional.of(detailIn(3L)));

        assertThatThrownBy(() -> inventoryQueryService.getInventory(1L, manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThatThrownBy(() -> inventoryQueryService.getTransactionsOf(1L, new InventoryTransactionSearchCondition(1L, null, null, null, null, null, null, null, null, null), manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        verify(inventoryQueryRepository, never()).findTransactions(org.mockito.ArgumentMatchers.any());

        when(inventoryQueryRepository.findDetail(2L)).thenReturn(Optional.of(detailIn(1L)));
        assertThat(inventoryQueryService.getInventory(2L, manager).warehouseId()).isEqualTo(1L);
    }
}
