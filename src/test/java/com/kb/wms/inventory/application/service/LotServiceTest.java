package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Mock
    private LotRepository lotRepository;
    @Mock
    private InventoryQueryRepository inventoryQueryRepository;

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
}
