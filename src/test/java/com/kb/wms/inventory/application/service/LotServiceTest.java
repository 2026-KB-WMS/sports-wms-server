package com.kb.wms.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.exception.InventoryErrorCode;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock
    private LotRepository lotRepository;
    @Mock
    private InventoryQueryRepository inventoryQueryRepository;

    @InjectMocks
    private LotService lotService;

    @Test
    @DisplayName("존재하는 skuId로 필터링하면 로트 목록을 조회한다")
    void getLots_success() {
        LotSearchCondition condition = new LotSearchCondition(1L, null, null, null);
        when(inventoryQueryRepository.existsSku(1L)).thenReturn(true);
        List<LotSummary> expected = List.of();
        when(inventoryQueryRepository.findLots(condition)).thenReturn(expected);

        List<LotSummary> result = lotService.getLots(condition);

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("존재하지 않는 skuId로 필터링하면 NOT_FOUND 예외를 던진다")
    void getLots_skuNotFound() {
        LotSearchCondition condition = new LotSearchCondition(999L, null, null, null);
        when(inventoryQueryRepository.existsSku(999L)).thenReturn(false);

        assertThatThrownBy(() -> lotService.getLots(condition))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
        verify(inventoryQueryRepository, never()).findLots(condition);
    }

    @Test
    @DisplayName("존재하지 않는 로트를 조회하면 NOT_FOUND 예외를 던진다")
    void getLot_notFound() {
        when(inventoryQueryRepository.findLot(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getLot(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.NOT_FOUND.name());
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
}
