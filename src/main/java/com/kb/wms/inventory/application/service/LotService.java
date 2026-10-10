package com.kb.wms.inventory.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.LotUseCase;
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
import com.kb.wms.inventory.domain.enums.LotStatus;
import com.kb.wms.inventory.exception.InventoryErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.statushistory.domain.enums.StatusHistoryEntityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotService implements LotUseCase {

    private static final int STATUS_REASON_MAX_LENGTH = 500;

    private final LotRepository lotRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryQueryRepository inventoryQueryRepository;
    private final StatusHistoryUseCase statusHistoryUseCase;

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
     * 로트 행을 먼저, 그다음 이 로트의 재고 행을 id 오름차순으로 잠근 뒤 수량을 확인하고 상태를 바꾼다.
     * 할당·조정은 재고 행만 잠그므로, 재고 행을 잠근 상태에서 수량 0을 확인하면 그 사이에 수량이 바뀌지 않는다.
     */
    @Override
    @Transactional
    public void changeLotStatus(Long lotId, LotStatusChangeCommand command) {
        LotStatus target = command.status();
        if (target == null || target == LotStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "변경할 상태는 AVAILABLE, QUARANTINED, DISPOSED 중 하나여야 합니다.");
        }
        String reason = command.reason() == null ? "" : command.reason().strip();
        if (reason.isEmpty() || reason.length() > STATUS_REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "변경 사유는 1자 이상 " + STATUS_REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (command.userId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "처리 사용자는 필수입니다.");
        }

        Lot lot = lotRepository.findByIdForUpdate(lotId)
                .orElseThrow(() -> new BusinessException(InventoryErrorCode.LOT_NOT_FOUND));
        if (!lot.canTransitionTo(target)) {
            throw new BusinessException(InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION,
                    lot.getStatus() + "에서 " + target + "(으)로 바꿀 수 없습니다.");
        }
        if (target == LotStatus.AVAILABLE && lot.isExpiredAt(LocalDate.now())) {
            throw new BusinessException(InventoryErrorCode.INVALID_LOT_STATUS_TRANSITION,
                    "유통기한이 지난 로트는 가용 상태로 되돌릴 수 없습니다.");
        }

        List<InventoryLot> rows = inventoryLotRepository.findAllByLotIdForUpdate(lotId);
        if (target == LotStatus.DISPOSED && rows.stream()
                .anyMatch(row -> row.getOnHandQuantity() > 0 || row.getAllocatedQuantity() > 0)) {
            throw new BusinessException(InventoryErrorCode.LOT_HAS_STOCK);
        }
        if (target == LotStatus.QUARANTINED && rows.stream().anyMatch(row -> row.getAllocatedQuantity() > 0)) {
            throw new BusinessException(InventoryErrorCode.LOT_HAS_ALLOCATION);
        }

        LotStatus before = lot.getStatus();
        switch (target) {
            case QUARANTINED -> lot.quarantine();
            case DISPOSED -> lot.dispose();
            default -> lot.release();
        }
        lotRepository.save(lot);
        statusHistoryUseCase.record(StatusHistoryEntityType.LOT, lotId,
                before.name(), target.name(), reason, command.userId());
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
