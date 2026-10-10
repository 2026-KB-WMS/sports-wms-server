package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.command.LotStatusChangeCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.exception.InventoryErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Mock
    private LotRepository lotRepository;
    @Mock
    private InventoryLotRepository inventoryLotRepository;
    @Mock
    private InventoryQueryRepository inventoryQueryRepository;
    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;

    @InjectMocks
    private LotService lotService;

    @Test
    @DisplayName("존재하는 skuId로 필터링하면 로트 목록을 조회한다")
    void getLots_success() {
        LotSearchCondition condition = LotSearchCondition.unscoped(1L, null, null, null);
        when(inventoryQueryRepository.existsSku(1L)).thenReturn(true);
        List<LotSummary> expected = List.of();
        when(inventoryQueryRepository.findLots(condition)).thenReturn(expected);

        List<LotSummary> result = lotService.getLots(condition, HQ);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 skuId로 필터링하면 SKU_NOT_FOUND 예외를 던진다")
    void getLots_skuNotFound() {
        LotSearchCondition condition = LotSearchCondition.unscoped(999L, null, null, null);
        when(inventoryQueryRepository.existsSku(999L)).thenReturn(false);

        assertThatThrownBy(() -> lotService.getLots(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.SKU_NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findLots(condition);
    }

    @Test
    @DisplayName("존재하지 않는 supplierId로 필터링하면 SUPPLIER_NOT_FOUND 예외를 던진다")
    void getLots_supplierNotFound() {
        LotSearchCondition condition = LotSearchCondition.unscoped(null, 999L, null, null);
        when(inventoryQueryRepository.existsSupplier(999L)).thenReturn(false);

        assertThatThrownBy(() -> lotService.getLots(condition, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.SUPPLIER_NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findLots(condition);
    }

    @Test
    @DisplayName("존재하는 supplierId로 필터링하면 로트 목록을 조회한다")
    void getLots_supplierExists() {
        LotSearchCondition condition = LotSearchCondition.unscoped(null, 3L, null, null);
        when(inventoryQueryRepository.existsSupplier(3L)).thenReturn(true);
        List<LotSummary> expected = List.of();
        when(inventoryQueryRepository.findLots(condition)).thenReturn(expected);

        assertThat(lotService.getLots(condition, HQ)).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 로트를 조회하면 LOT_NOT_FOUND 예외를 던진다")
    void getLot_notFound() {
        when(inventoryQueryRepository.findLot(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getLot(999L, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_FOUND.name());
    }

    @Test
    @DisplayName("존재하지 않는 로트의 입고 이력을 조회하면 LOT_NOT_FOUND 예외를 던진다")
    void getLotInbounds_lotNotFound() {
        when(lotRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> lotService.getLotInbounds(999L, HQ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findLotInbounds(999L);
    }

    @Test
    @DisplayName("존재하는 로트의 입고 이력을 조회하면 쿼리 결과를 그대로 반환한다")
    void getLotInbounds_success() {
        when(lotRepository.existsById(1L)).thenReturn(true);
        List<LotInboundView> expected = List.of(new LotInboundView(
                7L, "IB-20261001-0001", 4L, LocalDateTime.of(2026, 10, 1, 14, 30),
                100L, 95L, 5L, BigDecimal.valueOf(1200)));
        when(inventoryQueryRepository.findLotInbounds(1L)).thenReturn(expected);

        assertThat(lotService.getLotInbounds(1L, HQ)).isEqualTo(expected);
    }

    @Test
    @DisplayName("유통기한이 제조일보다 이전이면 VALIDATION_ERROR 예외를 던진다")
    void findOrRegister_expiryBeforeManufactured() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), BigDecimal.TEN);

        assertThatThrownBy(() -> lotService.findOrRegister(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(lotRepository, never()).findBySkuIdAndSupplierIdAndLotNumber(any(), any(), any());
    }

    @Test
    @DisplayName("같은 SKU·공급처·로트 번호의 로트가 없으면 새로 등록한다")
    void findOrRegister_registersNew() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        when(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(1L, 1L, "LOT-001")).thenReturn(Optional.empty());
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lot result = lotService.findOrRegister(command);

        assertThat(result.getLotNumber()).isEqualTo("LOT-001");
        assertThat(result.getUnitCost()).isEqualByComparingTo(BigDecimal.TEN);
    }

    @Test
    @DisplayName("기존 로트와 원가·일자가 일치하면 기존 로트를 재사용한다")
    void findOrRegister_reusesExisting() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        Lot existing = Lot.register(1L, 1L, "LOT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        when(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(1L, 1L, "LOT-001")).thenReturn(Optional.of(existing));

        Lot result = lotService.findOrRegister(command);

        assertThat(result).isSameAs(existing);
        verify(lotRepository, never()).save(any());
    }

    @Test
    @DisplayName("가용 상태가 아닌 기존 로트를 재사용하려 하면 LOT_NOT_AVAILABLE 예외를 던진다")
    void findOrRegister_existingNotAvailable() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        Lot existing = Lot.register(1L, 1L, "LOT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        existing.quarantine();
        when(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(1L, 1L, "LOT-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> lotService.findOrRegister(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_NOT_AVAILABLE.name());
    }

    @Test
    @DisplayName("기존 로트와 원가가 다르면 LOT_UNIT_COST_MISMATCH 예외를 던진다")
    void findOrRegister_unitCostMismatch() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.valueOf(20));
        Lot existing = Lot.register(1L, 1L, "LOT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        when(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(1L, 1L, "LOT-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> lotService.findOrRegister(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_UNIT_COST_MISMATCH.name());
    }

    @Test
    @DisplayName("기존 로트와 제조일·유통기한이 다르면 LOT_DATE_MISMATCH 예외를 던진다")
    void findOrRegister_dateMismatch() {
        LotRegisterCommand command = new LotRegisterCommand(1L, 1L, "LOT-001",
                LocalDate.of(2026, 2, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        Lot existing = Lot.register(1L, 1L, "LOT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), BigDecimal.TEN);
        when(lotRepository.findBySkuIdAndSupplierIdAndLotNumber(1L, 1L, "LOT-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> lotService.findOrRegister(command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(InventoryErrorCode.LOT_DATE_MISMATCH.name());
    }


    private final AuthenticatedUser manager =
            new AuthenticatedUser(2L, UserRole.WAREHOUSE_MANAGER, List.of(1L), List.of());

    private static LotSummary lotSummary() {
        return new LotSummary(5L, "LOT-001", 2L, "SKU-001", "상품A", 3L, "한빛식품",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LotStatus.AVAILABLE, BigDecimal.TEN, null, null);
    }

    @Test
    @DisplayName("창고 관리자의 로트 목록은 담당 창고 범위로 조회한다")
    void getLots_warehouseManager_scoped() {
        when(inventoryQueryRepository.findLots(new LotSearchCondition(null, null, null, null, List.of(1L))))
                .thenReturn(List.of(lotSummary()));

        assertThat(lotService.getLots(LotSearchCondition.unscoped(null, null, null, null), manager)).hasSize(1);
    }

    @Test
    @DisplayName("담당 창고에 재고도 입고 완료 이력도 없는 로트는 창고 관리자에게 403이고, 본사에는 보인다")
    void getLot_notVisibleToWarehouseManager_forbidden() {
        when(inventoryQueryRepository.findLot(5L)).thenReturn(Optional.of(lotSummary()));
        when(inventoryQueryRepository.findLot(5L, List.of(1L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getLot(5L, manager))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(lotService.getLot(5L, HQ).lotNumber()).isEqualTo("LOT-001");
    }

    @Test
    @DisplayName("담당 창고에 이력이 있는 로트는 창고 관리자에게 보인다")
    void getLot_visibleToWarehouseManager() {
        when(inventoryQueryRepository.findLot(5L)).thenReturn(Optional.of(lotSummary()));
        when(inventoryQueryRepository.findLot(5L, List.of(1L))).thenReturn(Optional.of(lotSummary()));

        assertThat(lotService.getLot(5L, manager).lotNumber()).isEqualTo("LOT-001");
    }

    @Test
    @DisplayName("창고 관리자의 로트 입고 이력은 담당 창고의 입고만 남긴다")
    void getLotInbounds_warehouseManager_filtered() {
        when(lotRepository.existsById(5L)).thenReturn(true);
        LotInboundView mine = new LotInboundView(7L, "IB-1", 1L, LocalDateTime.of(2026, 10, 1, 14, 30),
                100L, 95L, 5L, BigDecimal.valueOf(1200));
        LotInboundView others = new LotInboundView(8L, "IB-2", 9L, LocalDateTime.of(2026, 10, 2, 14, 30),
                10L, 10L, 0L, BigDecimal.valueOf(1200));
        when(inventoryQueryRepository.findLotInbounds(5L)).thenReturn(List.of(mine, others));

        assertThat(lotService.getLotInbounds(5L, manager)).containsExactly(mine);
    }

    // ---------- 상태 변경 ----------

    private static Lot lotIn(LotStatus status, LocalDate expiryDate) {
        return Lot.builder().lotId(5L).skuId(2L).supplierId(3L).lotNumber("LOT-001")
                .expiryDate(expiryDate).status(status).unitCost(BigDecimal.TEN).build();
    }

    private static InventoryLot row(long onHand, long allocated) {
        return InventoryLot.builder().inventoryLotId(10L).sectionId(1L).lotId(5L)
                .onHandQuantity(onHand).allocatedQuantity(allocated).build();
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName()).isEqualTo(code);
    }

    @Test
    @DisplayName("가용 로트를 격리하면 상태가 바뀌고 이전·이후 상태와 사유가 이력에 남는다")
    void changeStatus_quarantine_success() {
        when(lotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lotIn(LotStatus.AVAILABLE, null)));
        when(inventoryLotRepository.findAllByLotIdForUpdate(5L)).thenReturn(List.of(row(10, 0)));

        lotService.changeLotStatus(5L, new LotStatusChangeCommand(LotStatus.QUARANTINED, " 품질 이상 ", 1L));

        org.mockito.ArgumentCaptor<Lot> saved = org.mockito.ArgumentCaptor.forClass(Lot.class);
        verify(lotRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(LotStatus.QUARANTINED);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.LOT, 5L, "AVAILABLE", "QUARANTINED", "품질 이상", 1L);
    }

    @Test
    @DisplayName("할당 수량이 남아 있으면 격리할 수 없다(LOT_HAS_ALLOCATION)")
    void changeStatus_quarantine_withAllocation() {
        when(lotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lotIn(LotStatus.AVAILABLE, null)));
        when(inventoryLotRepository.findAllByLotIdForUpdate(5L)).thenReturn(List.of(row(10, 0), row(5, 2)));

        assertCode(() -> lotService.changeLotStatus(5L,
                new LotStatusChangeCommand(LotStatus.QUARANTINED, "사유", 1L)),
                InventoryErrorCode.LOT_HAS_ALLOCATION.name());
        verify(lotRepository, never()).save(any());
        verifyNoInteractions(statusHistoryUseCase);
    }

    @Test
    @DisplayName("격리 로트는 유통기한이 지나지 않았으면 가용으로 되돌린다")
    void changeStatus_release_success() {
        when(lotRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(lotIn(LotStatus.QUARANTINED, LocalDate.now().plusDays(30))));
        when(inventoryLotRepository.findAllByLotIdForUpdate(5L)).thenReturn(List.of());

        lotService.changeLotStatus(5L, new LotStatusChangeCommand(LotStatus.AVAILABLE, "재검사 통과", 1L));

        verify(statusHistoryUseCase).record(StatusHistoryEntityType.LOT, 5L, "QUARANTINED", "AVAILABLE", "재검사 통과", 1L);
    }

    @Test
    @DisplayName("유통기한이 지난 격리 로트는 가용으로 되돌릴 수 없다")
    void changeStatus_release_expired() {
        when(lotRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(lotIn(LotStatus.QUARANTINED, LocalDate.now().minusDays(1))));

        assertCode(() -> lotService.changeLotStatus(5L,
                new LotStatusChangeCommand(LotStatus.AVAILABLE, "사유", 1L)),
                InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION.name());
        verify(lotRepository, never()).save(any());
    }

    @Test
    @DisplayName("수량이 모두 0이면 만료 로트도 폐기할 수 있다")
    void changeStatus_dispose_success() {
        when(lotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lotIn(LotStatus.EXPIRED, null)));
        when(inventoryLotRepository.findAllByLotIdForUpdate(5L)).thenReturn(List.of(row(0, 0)));

        lotService.changeLotStatus(5L, new LotStatusChangeCommand(LotStatus.DISPOSED, "폐기 처리", 1L));

        verify(statusHistoryUseCase).record(StatusHistoryEntityType.LOT, 5L, "EXPIRED", "DISPOSED", "폐기 처리", 1L);
    }

    @Test
    @DisplayName("보유 수량이 남아 있으면 폐기할 수 없다(LOT_HAS_STOCK)")
    void changeStatus_dispose_hasStock() {
        when(lotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lotIn(LotStatus.AVAILABLE, null)));
        when(inventoryLotRepository.findAllByLotIdForUpdate(5L)).thenReturn(List.of(row(0, 0), row(3, 0)));

        assertCode(() -> lotService.changeLotStatus(5L,
                new LotStatusChangeCommand(LotStatus.DISPOSED, "사유", 1L)), InventoryErrorCode.LOT_HAS_STOCK.name());
        verify(lotRepository, never()).save(any());
    }

    @Test
    @DisplayName("폐기된 로트, 같은 상태로의 변경, 만료 로트의 격리는 INVALID_LOT_STATUS_TRANSITION이다")
    void changeStatus_invalidTransitions() {
        when(lotRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lotIn(LotStatus.DISPOSED, null)));
        assertCode(() -> lotService.changeLotStatus(5L,
                new LotStatusChangeCommand(LotStatus.AVAILABLE, "사유", 1L)),
                InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION.name());

        when(lotRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(lotIn(LotStatus.AVAILABLE, null)));
        assertCode(() -> lotService.changeLotStatus(6L,
                new LotStatusChangeCommand(LotStatus.AVAILABLE, "사유", 1L)),
                InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION.name());

        when(lotRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(lotIn(LotStatus.EXPIRED, null)));
        assertCode(() -> lotService.changeLotStatus(7L,
                new LotStatusChangeCommand(LotStatus.QUARANTINED, "사유", 1L)),
                InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION.name());
        verify(lotRepository, never()).save(any());
        verifyNoInteractions(inventoryLotRepository);
    }

    @Test
    @DisplayName("EXPIRED·누락 상태와 빈·긴 사유는 로트를 조회하기 전에 VALIDATION_ERROR다")
    void changeStatus_validation() {
        assertCode(() -> lotService.changeLotStatus(5L, new LotStatusChangeCommand(LotStatus.EXPIRED, "사유", 1L)),
                ErrorCode.VALIDATION_ERROR.name());
        assertCode(() -> lotService.changeLotStatus(5L, new LotStatusChangeCommand(null, "사유", 1L)),
                ErrorCode.VALIDATION_ERROR.name());
        assertCode(() -> lotService.changeLotStatus(5L, new LotStatusChangeCommand(LotStatus.DISPOSED, "  ", 1L)),
                ErrorCode.VALIDATION_ERROR.name());
        assertCode(() -> lotService.changeLotStatus(5L,
                new LotStatusChangeCommand(LotStatus.DISPOSED, "가".repeat(501), 1L)), ErrorCode.VALIDATION_ERROR.name());
        verifyNoInteractions(lotRepository);
    }

    @Test
    @DisplayName("없는 로트는 LOT_NOT_FOUND다")
    void changeStatus_notFound() {
        when(lotRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertCode(() -> lotService.changeLotStatus(999L,
                new LotStatusChangeCommand(LotStatus.DISPOSED, "사유", 1L)), InventoryErrorCode.LOT_NOT_FOUND.name());
    }
}

