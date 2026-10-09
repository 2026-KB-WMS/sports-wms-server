package com.kb.wms.inventory.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.command.LotRegisterCommand;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
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

    private final LotRepository lotRepository;
    private final InventoryQueryRepository inventoryQueryRepository;

    /**
     * 필터로 준 skuId·supplierId가 존재하지 않으면 404(SKU_NOT_FOUND·SUPPLIER_NOT_FOUND)로 응답한다.
     * 존재 확인은 조회 전용 포트(InventoryQueryRepository)의 읽기 쿼리로 하며 다른 도메인 서비스를 거치지 않는다.
     */
    @Override
    public List<LotSummary> getLots(LotSearchCondition condition, AuthenticatedUser actor) {
        if (condition.skuId() != null && !inventoryQueryRepository.existsSku(condition.skuId())) {
            throw new BusinessException(InventoryErrorCode.SKU_NOT_FOUND);
        }
        if (condition.supplierId() != null && !inventoryQueryRepository.existsSupplier(condition.supplierId())) {
            throw new BusinessException(InventoryErrorCode.SUPPLIER_NOT_FOUND);
        }
        return inventoryQueryRepository.findLots(new LotSearchCondition(condition.skuId(), condition.supplierId(),
                condition.expiringBefore(), condition.keyword(), actor.warehouseScope(null)));
    }

    @Override
    public LotSummary getLot(Long lotId, AuthenticatedUser actor) {
        LotSummary lot = inventoryQueryRepository.findLot(lotId)
                .orElseThrow(() -> new BusinessException(InventoryErrorCode.LOT_NOT_FOUND));
        // 목록과 같은 기준: 담당 창고에 재고 또는 입고 완료 이력이 없는 로트는 창고 관리자에게 보이지 않는다.
        List<Long> scope = actor.warehouseScope(null);
        if (scope != null && inventoryQueryRepository.findLot(lotId, scope).isEmpty()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return lot;
    }

    @Override
    public List<LotInboundView> getLotInbounds(Long lotId, AuthenticatedUser actor) {
        if (!lotRepository.existsById(lotId)) {
            throw new BusinessException(InventoryErrorCode.LOT_NOT_FOUND);
        }
        List<Long> scope = actor.warehouseScope(null);
        return inventoryQueryRepository.findLotInbounds(lotId).stream()
                .filter(inbound -> scope == null || scope.contains(inbound.warehouseId()))
                .toList();
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
