package com.kb.wms.inventory.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.command.InventoryAdjustCommand;
import com.kb.wms.inventory.application.port.in.result.InventoryAdjustmentResult;
import com.kb.wms.inventory.application.port.out.InventoryLotRepository;
import com.kb.wms.inventory.application.port.out.InventoryTransactionRepository;
import com.kb.wms.inventory.application.port.out.SectionCapacityPort;
import com.kb.wms.inventory.domain.entity.InventoryLot;
import com.kb.wms.inventory.domain.entity.InventoryTransaction;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;
import com.kb.wms.inventory.exception.InventoryErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryAdjustmentService implements InventoryAdjustmentUseCase {

    private final InventoryLotRepository inventoryLotRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SectionCapacityPort sectionCapacityPort;

    /**
     * 재고 행을 잠근 뒤 화면에서 본 수량(beforeQuantity)이 현재 값과 같을 때만 조정한다.
     * 같은 요청이 중복 전송돼도 두 번째는 STALE_QUANTITY로 거절되어 중복 반영되지 않는다.
     * 수량 변경, 구역 사용 용량 증감, 이력 기록은 한 트랜잭션이다.
     */
    @Override
    public InventoryAdjustmentResult adjust(InventoryAdjustCommand command) {
        validate(command);

        InventoryLot inventoryLot = inventoryLotRepository.findByIdForUpdate(command.inventoryLotId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, InventoryQueryService.INVENTORY_NOT_FOUND_MESSAGE));

        long before = inventoryLot.getOnHandQuantity();
        long after = command.afterQuantity();
        if (before != command.beforeQuantity()) {
            throw new BusinessException(InventoryErrorCode.STALE_QUANTITY);
        }
        if (after < inventoryLot.getAllocatedQuantity()) {
            throw new BusinessException(InventoryErrorCode.BELOW_ALLOCATED_QUANTITY,
                    "보유 수량은 할당 수량(" + inventoryLot.getAllocatedQuantity() + ")보다 작게 조정할 수 없습니다.");
        }

        long delta = after - before;
        if (delta > 0) {
            sectionCapacityPort.occupy(inventoryLot.getSectionId(), delta);
            inventoryLot.increase(delta);
        } else {
            sectionCapacityPort.vacate(inventoryLot.getSectionId(), -delta);
            inventoryLot.decrease(-delta);
        }
        // 실사 조정 = 재고 조정이므로 마지막 실사 시각을 갱신한다.
        inventoryLot.markCounted(LocalDateTime.now());
        InventoryLot saved = inventoryLotRepository.save(inventoryLot);

        InventoryTransaction transaction = inventoryTransactionRepository.save(InventoryTransaction.record(
                saved.getInventoryLotId(), TransactionType.ADJUSTMENT, before, after,
                ReferenceType.ADJUSTMENT, null, command.reason().trim(), command.userId()));

        return new InventoryAdjustmentResult(transaction, saved);
    }

    private static void validate(InventoryAdjustCommand command) {
        if (command.afterQuantity() == null || command.afterQuantity() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조정 후 수량은 0 이상이어야 합니다.");
        }
        if (command.beforeQuantity() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "현재 보유 수량(beforeQuantity)은 필수입니다.");
        }
        if (command.beforeQuantity().equals(command.afterQuantity())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조정 전후 수량이 같아 변경할 내용이 없습니다.");
        }
        if (!StringUtils.hasText(command.reason())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조정 사유는 필수입니다.");
        }
    }
}
