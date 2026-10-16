package com.kb.wms.inbound.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.PurchaseOrderUseCase;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderCancelCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderConfirmCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.PurchaseOrderRejectCommand;
import com.kb.wms.inbound.application.port.in.query.PurchaseOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderConfirmResult;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderDetails;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.application.port.out.InboundRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SkuPurchasePricePort;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;
import com.kb.wms.inbound.domain.entity.SkuSupplier;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.InboundHistoryEntityType;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 발주(요청) 등록·조회·확정·반려·취소.
 *
 * <p>확정하면 라인별 공급처로 공급처 발주서를 공급처당 한 건씩 만든다(ADR-030).
 *
 * <p>역할은 SecurityConfig가 검사하고, 담당 창고 범위와 취소의 작성자·상태별 권한(요청 발주는 작성자 창고 관리자,
 * 확정 발주는 본사 관리자)은 이 서비스가 검사한다(ADR-012).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService implements PurchaseOrderUseCase {

    private static final String NO_PREFIX = "PO-";
    private static final String SUPPLIER_ORDER_NO_PREFIX = "SO-";
    private static final DateTimeFormatter NO_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final int CANCEL_REASON_MAX_LENGTH = 500;
    private static final int REASON_MAX_LENGTH = 500;

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InboundRepository inboundRepository;
    private final PurchaseOrderQueryRepository purchaseOrderQueryRepository;
    private final SupplierRepository supplierRepository;
    private final SkuSupplierRepository skuSupplierRepository;
    private final SupplierOrderRepository supplierOrderRepository;
    private final SupplierOrderQueryRepository supplierOrderQueryRepository;
    private final WarehouseAvailabilityPort warehouseAvailabilityPort;
    private final SkuPurchasePricePort skuPurchasePricePort;
    private final StatusHistoryUseCase statusHistoryUseCase;

    @Override
    @Transactional
    public Long registerPurchaseOrder(PurchaseOrderRegisterCommand command, AuthenticatedUser actor) {
        validateRegister(command);
        actor.requireWarehouseAccess(command.warehouseId());

        warehouseAvailabilityPort.requireActive(command.warehouseId());

        // 발주 단가는 요청에서 받지 않고 등록 시점의 SKU 매입 단가를 스냅샷한다.
        List<BigDecimal> unitPrices = command.lines().stream()
                .map(line -> skuPurchasePricePort.getPurchasablePrice(line.skuId()))
                .toList();

        PurchaseOrder saved = purchaseOrderRepository.save(PurchaseOrder.register(
                nextPurchaseOrderNo(LocalDate.now()), command.warehouseId(),
                command.expectedAt(), command.note(), command.createdBy()));

        List<PurchaseOrderLine> lines = new ArrayList<>(command.lines().size());
        for (int i = 0; i < command.lines().size(); i++) {
            PurchaseOrderRegisterCommand.Line line = command.lines().get(i);
            lines.add(PurchaseOrderLine.register(
                    saved.getPurchaseOrderId(), line.skuId(), line.expectedQuantity(), unitPrices.get(i)));
        }
        purchaseOrderRepository.saveLines(lines);

        statusHistoryUseCase.record(InboundHistoryEntityType.PURCHASE_ORDER.name(), saved.getPurchaseOrderId(),
                null, PurchaseOrderStatus.REQUESTED.name(), null, command.createdBy());

        return saved.getPurchaseOrderId();
    }

    @Override
    public PageResult<PurchaseOrderSummary> getPurchaseOrders(PurchaseOrderSearchCondition condition,
                                                              PageCondition page, AuthenticatedUser actor) {
        List<Long> scope = actor.warehouseScope(condition.warehouseId());
        if (condition.createdFrom() != null && condition.createdTo() != null
                && condition.createdFrom().isAfter(condition.createdTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "등록 시작 일시는 종료 일시보다 늦을 수 없습니다.");
        }
        // 담당 창고 범위 검사(403)를 통과한 뒤에만 존재 여부를 확인해, 비담당자에게 존재 여부를 흘리지 않는다.
        if (condition.warehouseId() != null) {
            warehouseAvailabilityPort.requireExists(condition.warehouseId());
        }
        if (condition.supplierId() != null && !supplierRepository.existsById(condition.supplierId())) {
            throw new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND);
        }
        PageResult<PurchaseOrderSummary> result = purchaseOrderQueryRepository.search(new PurchaseOrderSearchCondition(
                condition.status(), condition.warehouseId(), condition.supplierId(), condition.keyword(),
                condition.createdFrom(), condition.createdTo(), scope), page);
        return PageResult.of(withSuppliers(result.items()), page, result.totalElements());
    }

    @Override
    public PurchaseOrderView getPurchaseOrder(Long purchaseOrderId, AuthenticatedUser actor) {
        PurchaseOrderView view = purchaseOrderQueryRepository.findView(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
        actor.requireWarehouseAccess(view.warehouseId());
        PurchaseOrderView result =
                view.withSupplierOrders(supplierOrderQueryRepository.findSummariesByPurchaseOrderId(purchaseOrderId));
        // 취소·반려 사유는 해당 상태로 바뀔 때의 상태 이력에서 읽는다. 사유 없이 취소했으면 null.
        if (view.status() == PurchaseOrderStatus.CANCELED) {
            return result.withCancelReason(findReason(purchaseOrderId, PurchaseOrderStatus.CANCELED));
        }
        if (view.status() == PurchaseOrderStatus.REJECTED) {
            return result.withRejectReason(findReason(purchaseOrderId, PurchaseOrderStatus.REJECTED));
        }
        return result;
    }

    @Override
    public PurchaseOrderDetails getPurchaseOrderDetails(Long purchaseOrderId, AuthenticatedUser actor) {
        PurchaseOrderView view = getPurchaseOrder(purchaseOrderId, actor);
        return new PurchaseOrderDetails(
                view.purchaseOrderId(), view.purchaseOrderNo(), view.status(),
                purchaseOrderQueryRepository.findLineViews(purchaseOrderId));
    }

    /**
     * 발주 행을 락으로 잡고, 라인별 공급처(요청에서 지정한 값 또는 이미 배정된 값)를 검증한 뒤
     * 공급처별로 발주서를 한 건씩 만들어 라인을 연결하고 발주를 CONFIRMED로 바꾼다. 한 트랜잭션이라 하나라도 실패하면
     * 아무것도 반영되지 않는다. 공급처는 해당 SKU에 매핑된 활성 공급처여야 한다.
     */
    @Override
    @Transactional
    public PurchaseOrderConfirmResult confirmPurchaseOrder(Long purchaseOrderId,
                                                           PurchaseOrderConfirmCommand command) {
        requireUser(command == null ? null : command.userId());
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
        if (!purchaseOrder.isRequested()) {
            throw new BusinessException(ErrorCode.CONFLICT, "확정 대기 상태의 발주만 확정할 수 있습니다.");
        }

        List<PurchaseOrderLine> lines =
                purchaseOrderRepository.findLinesByPurchaseOrderIdForUpdate(purchaseOrderId);
        Map<Long, Long> overrides = resolveSupplierOverrides(lines, command.lines());
        Map<Long, Long> supplierByLine = effectiveSuppliers(lines, overrides);

        requireActiveSuppliers(supplierByLine);
        requireMappedSuppliers(lines, supplierByLine);

        // 공급처 ID 순으로 발주서를 만들어 번호와 연결 순서를 결정적으로 유지한다.
        Map<Long, List<PurchaseOrderLine>> bySupplier = new TreeMap<>();
        for (PurchaseOrderLine line : lines) {
            Long supplierId = supplierByLine.get(line.getPurchaseOrderLineId());
            if (overrides.containsKey(line.getPurchaseOrderLineId())) {
                line.assignSupplier(supplierId);
            }
            bySupplier.computeIfAbsent(supplierId, id -> new ArrayList<>()).add(line);
        }

        String prefix = SUPPLIER_ORDER_NO_PREFIX + LocalDate.now().format(NO_DATE_FORMAT) + "-";
        long sequence = supplierOrderRepository.countBySupplierOrderNoPrefix(prefix);
        List<PurchaseOrderLine> attached = new ArrayList<>(lines.size());
        for (Map.Entry<Long, List<PurchaseOrderLine>> entry : bySupplier.entrySet()) {
            sequence++;
            SupplierOrder supplierOrder = supplierOrderRepository.save(SupplierOrder.issue(
                    prefix + String.format("%04d", sequence), purchaseOrderId, entry.getKey()));
            for (PurchaseOrderLine line : entry.getValue()) {
                line.attachToSupplierOrder(supplierOrder.getSupplierOrderId());
                attached.add(line);
            }
            statusHistoryUseCase.record(InboundHistoryEntityType.SUPPLIER_ORDER.name(),
                    supplierOrder.getSupplierOrderId(), null, SupplierOrderStatus.ISSUED.name(), null,
                    command.userId());
        }
        purchaseOrderRepository.saveLines(attached);

        purchaseOrder.confirm();
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        statusHistoryUseCase.record(InboundHistoryEntityType.PURCHASE_ORDER.name(), purchaseOrderId,
                PurchaseOrderStatus.REQUESTED.name(), saved.getStatus().name(), null, command.userId());
        return new PurchaseOrderConfirmResult(saved,
                supplierOrderQueryRepository.findSummariesByPurchaseOrderId(purchaseOrderId));
    }

    /**
     * 본사 관리자만 REQUESTED 발주를 반려한다. 소속 창고를 먼저 확인해 다른 창고 사용자에게 상태(409)가 드러나지 않게 하고,
     * 사유와 처리자는 REJECTED 전이 이력에 남긴다. 반려된 요청은 발주서가 만들어지지 않는다.
     */
    @Override
    @Transactional
    public PurchaseOrder rejectPurchaseOrder(Long purchaseOrderId, PurchaseOrderRejectCommand command,
                                             AuthenticatedUser actor) {
        requireUser(command == null ? null : command.userId());
        String reason = command.reason();
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주를 반려할 때는 사유를 입력해야 합니다.");
        }
        if (reason.length() > REASON_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "반려 사유는 " + REASON_MAX_LENGTH + "자 이하여야 합니다.");
        }
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
        actor.requireWarehouseAccess(purchaseOrder.getWarehouseId());
        if (!actor.isHqAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!purchaseOrder.isRequested()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "확정 대기 상태의 발주만 반려할 수 있습니다. 현재 상태: " + purchaseOrder.getStatus());
        }
        purchaseOrder.reject();
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        statusHistoryUseCase.record(InboundHistoryEntityType.PURCHASE_ORDER.name(), purchaseOrderId,
                PurchaseOrderStatus.REQUESTED.name(), saved.getStatus().name(), reason, command.userId());
        return saved;
    }

    /**
     * 발주 헤더를 락으로 잡은 뒤 취소되지 않은 입고가 있으면 409 PURCHASE_ORDER_HAS_INBOUND로 막는다.
     * 입고 등록도 같은 발주 행을 락으로 잡으므로 취소와 입고 등록이 동시에 통과하지 못한다.
     * 취소 사유와 처리자는 CANCELED 전이 이력(StatusHistory)에 남기고, 응답의 cancelReason은 이 이력에서 읽는다.
     */
    @Override
    @Transactional
    public PurchaseOrder cancelPurchaseOrder(Long purchaseOrderId, PurchaseOrderCancelCommand command,
                                             AuthenticatedUser actor) {
        requireUser(command == null ? null : command.userId());
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(PurchaseOrderService::notFound);
        // 소속을 먼저 확인해, 다른 창고 사용자에게 발주의 상태(409)가 드러나지 않게 한다.
        actor.requireWarehouseAccess(purchaseOrder.getWarehouseId());
        if (!purchaseOrder.isInProgress()) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "요청 또는 확정 상태의 발주만 취소할 수 있습니다. 현재 상태: " + purchaseOrder.getStatus());
        }
        requireCancelAuthority(purchaseOrder, actor);
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
        PurchaseOrderStatus from = purchaseOrder.getStatus();
        purchaseOrder.cancel();
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        statusHistoryUseCase.record(InboundHistoryEntityType.PURCHASE_ORDER.name(), purchaseOrderId,
                from.name(), saved.getStatus().name(), hasReason ? reason : null, command.userId());
        cancelSupplierOrders(purchaseOrderId, hasReason ? reason : null, command.userId());
        return saved;
    }

    /**
     * 요청 발주는 작성자인 창고 관리자만, 확정 발주는 본사 관리자만 취소할 수 있다.
     * 작성자도 발주 창고의 담당이어야 하며, 이는 호출 전에 소속 검사로 확인한다.
     */
    private static void requireCancelAuthority(PurchaseOrder purchaseOrder, AuthenticatedUser actor) {
        boolean allowed = purchaseOrder.getStatus() == PurchaseOrderStatus.REQUESTED
                ? actor.isWarehouseManager() && actor.userId().equals(purchaseOrder.getCreatedBy())
                : actor.isHqAdmin();
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /** 요청 취소를 공급처 발주서로 전파한다. 발주서는 확정으로만 생기므로 요청(REQUESTED) 취소에는 대상이 없다. */
    private void cancelSupplierOrders(Long purchaseOrderId, String reason, Long userId) {
        for (SupplierOrder supplierOrder : supplierOrderRepository.findByPurchaseOrderIdForUpdate(purchaseOrderId)) {
            if (!supplierOrder.isInProgress()) {
                continue;
            }
            SupplierOrderStatus from = supplierOrder.getStatus();
            supplierOrder.cancel();
            SupplierOrder saved = supplierOrderRepository.save(supplierOrder);
            statusHistoryUseCase.record(InboundHistoryEntityType.SUPPLIER_ORDER.name(),
                    saved.getSupplierOrderId(), from.name(), saved.getStatus().name(), reason, userId);
        }
    }

    /** 요청의 라인별 공급처 지정을 검증한다. 이 발주의 라인이 아니거나 중복이면 400. */
    private Map<Long, Long> resolveSupplierOverrides(List<PurchaseOrderLine> lines,
                                                     List<PurchaseOrderConfirmCommand.Line> requested) {
        Set<Long> lineIds = lines.stream().map(PurchaseOrderLine::getPurchaseOrderLineId).collect(Collectors.toSet());
        Map<Long, Long> overrides = new HashMap<>();
        for (PurchaseOrderConfirmCommand.Line line : requested) {
            if (line.purchaseOrderLineId() == null || line.supplierId() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "발주 항목 ID와 공급처 ID는 필수입니다.");
            }
            if (!lineIds.contains(line.purchaseOrderLineId())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "이 발주에 속하지 않은 발주 항목입니다. purchaseOrderLineId=" + line.purchaseOrderLineId());
            }
            if (overrides.put(line.purchaseOrderLineId(), line.supplierId()) != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "같은 발주 항목의 공급처를 중복해서 지정할 수 없습니다. purchaseOrderLineId="
                                + line.purchaseOrderLineId());
            }
        }
        return overrides;
    }

    /** 지정한 값이 있으면 그 값, 없으면 이미 배정된 값. 공급처가 없는 라인이 남으면 400 LINE_SUPPLIER_REQUIRED. */
    private static Map<Long, Long> effectiveSuppliers(List<PurchaseOrderLine> lines, Map<Long, Long> overrides) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (PurchaseOrderLine line : lines) {
            Long supplierId = overrides.getOrDefault(line.getPurchaseOrderLineId(), line.getSupplierId());
            if (supplierId == null) {
                throw new BusinessException(PurchaseOrderErrorCode.LINE_SUPPLIER_REQUIRED,
                        "공급처가 지정되지 않은 발주 항목이 있습니다. purchaseOrderLineId="
                                + line.getPurchaseOrderLineId());
            }
            result.put(line.getPurchaseOrderLineId(), supplierId);
        }
        return result;
    }

    /** 지정한 공급처가 존재하고 활성이어야 한다. */
    private void requireActiveSuppliers(Map<Long, Long> supplierByLine) {
        for (Long supplierId : new HashSet<>(supplierByLine.values())) {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
            if (!supplier.isActive()) {
                throw new BusinessException(PurchaseOrderErrorCode.SUPPLIER_INACTIVE,
                        "비활성 공급처는 지정할 수 없습니다. supplierId=" + supplierId);
            }
        }
    }

    /** 라인의 공급처는 그 SKU에 매핑된 공급처여야 한다. */
    private void requireMappedSuppliers(List<PurchaseOrderLine> lines, Map<Long, Long> supplierByLine) {
        Set<Long> skuIds = lines.stream().map(PurchaseOrderLine::getSkuId).collect(Collectors.toSet());
        Set<String> mapped = skuSupplierRepository.findBySkuIdIn(skuIds).stream()
                .map(mapping -> mappingKey(mapping.getSkuId(), mapping.getSupplierId()))
                .collect(Collectors.toSet());
        for (PurchaseOrderLine line : lines) {
            Long supplierId = supplierByLine.get(line.getPurchaseOrderLineId());
            if (!mapped.contains(mappingKey(line.getSkuId(), supplierId))) {
                throw new BusinessException(PurchaseOrderErrorCode.SKU_SUPPLIER_NOT_MAPPED,
                        "SKU에 매핑되지 않은 공급처입니다. skuId=" + line.getSkuId() + ", supplierId=" + supplierId);
            }
        }
    }

    private static String mappingKey(Long skuId, Long supplierId) {
        return skuId + "/" + supplierId;
    }

    /** 목록 항목에 라인에 배정된 공급처를 한 번의 조회로 채운다. */
    private List<PurchaseOrderSummary> withSuppliers(List<PurchaseOrderSummary> summaries) {
        if (summaries.isEmpty()) {
            return summaries;
        }
        Map<Long, List<PurchaseOrderSupplierView>> byOrder = purchaseOrderQueryRepository
                .findSuppliers(summaries.stream().map(PurchaseOrderSummary::purchaseOrderId).toList())
                .stream()
                .collect(Collectors.groupingBy(PurchaseOrderSupplierView::purchaseOrderId));
        return summaries.stream()
                .map(summary -> summary.withSuppliers(byOrder.getOrDefault(summary.purchaseOrderId(), List.of())))
                .toList();
    }

    private String findReason(Long purchaseOrderId, PurchaseOrderStatus status) {
        return statusHistoryUseCase
                .findStatusReason(InboundHistoryEntityType.PURCHASE_ORDER.name(), purchaseOrderId, status.name())
                .orElse(null);
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "처리 사용자는 필수입니다.");
        }
    }

    private void validateRegister(PurchaseOrderRegisterCommand command) {
        if (command.createdBy() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청 사용자는 필수입니다.");
        }
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

    private static BusinessException notFound() {
        return new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND);
    }
}
