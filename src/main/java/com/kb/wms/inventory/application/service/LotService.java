package com.kb.wms.inventory.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotSummary;
import com.kb.wms.inventory.application.port.out.InventoryQueryRepository;
import com.kb.wms.inventory.application.port.out.LotRepository;
import com.kb.wms.inventory.domain.entity.Lot;
import com.kb.wms.inventory.exception.InventoryErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotService implements LotUseCase {

    static final String LOT_NOT_FOUND_MESSAGE = "로트를 찾을 수 없습니다.";

    private final LotRepository lotRepository;
    private final InventoryQueryRepository inventoryQueryRepository;

    @Override
    public List<LotSummary> getLots(LotSearchCondition condition) {
        return inventoryQueryRepository.findLots(condition);
    }

    @Override
    public LotSummary getLot(Long lotId) {
        return inventoryQueryRepository.findLot(lotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, LOT_NOT_FOUND_MESSAGE));
    }

    /**
     * 같은 SKU·공급처·로트 번호의 로트가 있으면 원가·일자·상태가 요청과 맞는지 확인하고 재사용한다(ADR-004).
     * 같은 로트는 동일 원가를 가져야 하므로 원가가 다르면 거절한다.
     */
    @Override
    @Transactional
    public Lot findOrRegister(LotRegisterCommand command) {
        if (command.manufacturedDate() != null && command.expiryDate() != null
                && command.expiryDate().isBefore(command.manufacturedDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "유통기한은 제조일 이후여야 합니다.");
        }

        return lotRepository.findBySkuIdAndSupplierIdAndLotNumber(
                        command.skuId(), command.supplierId(), command.lotNumber())
                .map(existing -> verifySameLot(existing, command))
                .orElseGet(() -> lotRepository.save(Lot.register(
                        command.skuId(), command.supplierId(), command.lotNumber(),
                        command.manufacturedDate(), command.expiryDate(), command.unitCost())));
    }

    private static Lot verifySameLot(Lot existing, LotRegisterCommand command) {
        if (!existing.isAvailable()) {
            throw new BusinessException(InventoryErrorCode.LOT_NOT_AVAILABLE);
        }
        if (existing.getUnitCost().compareTo(command.unitCost()) != 0) {
            throw new BusinessException(InventoryErrorCode.LOT_UNIT_COST_MISMATCH);
        }
        if (!Objects.equals(existing.getManufacturedDate(), command.manufacturedDate())
                || !Objects.equals(existing.getExpiryDate(), command.expiryDate())) {
            throw new BusinessException(InventoryErrorCode.LOT_DATE_MISMATCH);
        }
        return existing;
    }
}
