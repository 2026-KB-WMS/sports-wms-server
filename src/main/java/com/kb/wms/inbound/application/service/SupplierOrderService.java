package com.kb.wms.inbound.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.SupplierOrderUseCase;
import com.kb.wms.inbound.application.port.in.query.SupplierOrderSearchCondition;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderDetails;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierOrderQueryRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.application.port.out.WarehouseAvailabilityPort;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.inbound.exception.SupplierOrderErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 공급처 발주서 조회. 역할은 SecurityConfig가, 담당 창고 범위는 이 서비스가 검사한다(ADR-012).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierOrderService implements SupplierOrderUseCase {

    private final SupplierOrderQueryRepository supplierOrderQueryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseAvailabilityPort warehouseAvailabilityPort;

    @Override
    public List<SupplierOrderSummary> getSupplierOrders(SupplierOrderSearchCondition condition,
                                                        AuthenticatedUser actor) {
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
        if (condition.purchaseOrderId() != null
                && purchaseOrderRepository.findById(condition.purchaseOrderId()).isEmpty()) {
            throw new BusinessException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND);
        }
        return supplierOrderQueryRepository.search(new SupplierOrderSearchCondition(
                condition.status(), condition.warehouseId(), condition.supplierId(), condition.purchaseOrderId(),
                condition.keyword(), condition.createdFrom(), condition.createdTo(), scope));
    }

    @Override
    public SupplierOrderDetails getSupplierOrder(Long supplierOrderId, AuthenticatedUser actor) {
        SupplierOrderView header = supplierOrderQueryRepository.findView(supplierOrderId)
                .orElseThrow(() -> new BusinessException(SupplierOrderErrorCode.SUPPLIER_ORDER_NOT_FOUND));
        actor.requireWarehouseAccess(header.warehouseId());
        return new SupplierOrderDetails(header, supplierOrderQueryRepository.findLineViews(supplierOrderId));
    }
}
