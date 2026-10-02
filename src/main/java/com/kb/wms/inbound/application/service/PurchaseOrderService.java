package com.kb.wms.inbound.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SkuPurchasePricePort;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 발주 등록·조회·확정·취소.
 *
 * <p>역할·소속 창고·작성자 검사(등록은 담당 창고 관리자, 확정은 본사 관리자, 취소는 상태별 권한자)는
 * 인증 도메인 연동 시 웹 어댑터에서 적용한다. 이 서비스는 상태·입력·참조 대상 규칙만 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService implements PurchaseOrderUseCase {

    private static final String NO_PREFIX = "PO-";
    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final int CANCEL_REASON_MAX_LENGTH = 500;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InboundRepository inboundRepository;
    private final PurchaseOrderQueryRepository purchaseOrderQueryRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseAvailabilityPort warehouseAvailabilityPort;
    private final SkuPurchasePricePort skuPurchasePricePort;

    @Override
    @Transactional
    public Long registerPurchaseOrder(PurchaseOrderRegisterCommand command) {
        validateRegister(command);

        warehouseAvailabilityPort.requireActive(command.warehouseId());

        Supplier supplier = supplierRepository.findById(command.supplierId())
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "비활성 공급처에는 발주를 등록할 수 없습니다.");
        }

        // 발주 단가는 요청에서 받지 않고 등록 시점의 SKU 매입 단가를 스냅샷한다.
        List<BigDecimal> unitPrices = command.lines().stream()
                .map(line -> skuPurchasePricePort.getPurchasablePrice(line.skuId()))
                .toList();

        PurchaseOrder saved = purchaseOrderRepository.save(PurchaseOrder.register(
                nextPurchaseOrderNo(LocalDate.now()), command.warehouseId(), command.supplierId(),
                command.expectedAt(), command.note(), command.createdBy()));

        List<PurchaseOrderLine> lines = new ArrayList<>(command.lines().size());
        for (int i = 0; i < command.lines().size(); i++) {
            PurchaseOrderRegisterCommand.Line line = command.lines().get(i);
            lines.add(PurchaseOrderLine.register(
                    saved.getPurchaseOrderId(), line.skuId(), line.expectedQuantity(), unitPrices.get(i)));
        }
        purchaseOrderRepository.saveLines(lines);

        return saved.getPurchaseOrderId();
    }

    @Override
    public List<PurchaseOrderSummary> getPurchaseOrders(PurchaseOrderSearchCondition condition) {
        if (condition.createdFrom() != null && condition.createdTo() != null
                && condition.createdFrom().isAfter(condition.createdTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "등록 시작 일시는 종료 일시보다 늦을 수 없습니다.");
        }
        return purchaseOrderQueryRepository.search(condition);
    }

    @Override
    public PurchaseOrderView getPurchaseOrder(Long purchaseOrderId) {
        return purchaseOrderQueryRepository.findView(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
    }

    @Override
    public PurchaseOrderDetails getPurchaseOrderDetails(Long purchaseOrderId) {
        PurchaseOrderView view = getPurchaseOrder(purchaseOrderId);
        return new PurchaseOrderDetails(
                view.purchaseOrderId(), view.purchaseOrderNo(), view.status(),
                purchaseOrderQueryRepository.findLineViews(purchaseOrderId));
    }

    @Override
    @Transactional
    public PurchaseOrder confirmPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = findOrThrow(purchaseOrderId);
        if (!purchaseOrder.isRequested()) {
            throw new BusinessException(ErrorCode.CONFLICT, "확정 대기 상태의 발주만 확정할 수 있습니다.");
        }
        Supplier supplier = supplierRepository.findById(purchaseOrder.getSupplierId())
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
        if (!supplier.isActive()) {
            throw new BusinessException(PurchaseOrderErrorCode.SUPPLIER_INACTIVE);
        }
        purchaseOrder.confirm();
        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * 발주 헤더를 락으로 잡은 뒤 취소되지 않은 입고가 있으면 409 PURCHASE_ORDER_HAS_INBOUND로 막는다.
     * 입고 등록도 같은 발주 행을 락으로 잡으므로 취소와 입고 등록이 동시에 통과하지 못한다.
     * 취소 사유 저장(StatusHistory)은 StatusHistory 도메인이 구현되면 추가한다.
     */
    @Override
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long purchaseOrderId, PurchaseOrderCancelCommand command) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
        if (!purchaseOrder.isInProgress()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "요청 또는 확정 상태의 발주만 취소할 수 있습니다. 현재 상태: " + purchaseOrder.getStatus());
        }
        String reason = command == null ? null : command.reason();
        boolean hasReason = reason != null && !reason.isBlank();
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CONFIRMED && !hasReason) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "확정된 발주를 취소할 때는 사유를 입력해야 합니다.");
        }
        if (hasReason && reason.length() > CANCEL_REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "취소 사유는 " + CANCEL_REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (inboundRepository.existsNotCanceledByPurchaseOrderId(purchaseOrderId)) {
            throw new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_HAS_INBOUND);
        }
        purchaseOrder.cancel();
        return purchaseOrderRepository.save(purchaseOrder);
    }

    private void validateRegister(PurchaseOrderRegisterCommand command) {
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 항목을 1개 이상 입력해주세요.");
        }
        Set<Long> skuIds = new HashSet<>();
        for (PurchaseOrderRegisterCommand.Line line : command.lines()) {
            if (line.expectedQuantity() <= 0) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 수량은 0보다 커야 합니다.");
            }
            if (!skuIds.add(line.skuId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "같은 SKU를 중복해서 발주할 수 없습니다. skuId=" + line.skuId());
            }
        }
        if (command.expectedAt() != null && !command.expectedAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "입고 예정 일시는 현재 시각 이후여야 합니다.");
        }
        if (command.note() != null && command.note().length() > NOTE_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "비고는 " + NOTE_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    /**
     * PO-YYYYMMDD-일련번호(4자리). 당일 발주 개수 + 1로 채번하며, 동시 등록으로 번호가 겹치면
     * UNIQUE 제약이 409 DUPLICATE_PURCHASE_ORDER_NO로 응답한다.
     */
    private String nextPurchaseOrderNo(LocalDate today) {
        String prefix = NO_PREFIX + today.format(NO_DATE_FORMAT) + "-";
        long sequence = purchaseOrderRepository.countByPurchaseOrderNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", sequence);
    }

    private PurchaseOrder findOrThrow(Long purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId).orElseThrow(PurchaseOrderService::notFound);
    }

    private static BusinessException notFound() {
        return new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }
}
