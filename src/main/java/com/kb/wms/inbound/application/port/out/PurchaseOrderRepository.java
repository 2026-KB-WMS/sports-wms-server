package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.domain.entity.PurchaseOrder;
import com.kb.wms.inbound.domain.entity.PurchaseOrderLine;

/**
 * 발주(헤더 + 항목) 영속성 아웃바운드 포트. 헤더와 항목은 한 트랜잭션에서 함께 저장한다.
 */
public interface PurchaseOrderRepository {

    PurchaseOrder save(PurchaseOrder purchaseOrder);

    /** 저장된 발주 헤더 ID로 항목을 연결해 저장한다. 항목의 purchaseOrderId는 호출 측이 채워 넘긴다. */
    List<PurchaseOrderLine> saveLines(List<PurchaseOrderLine> lines);

    Optional<PurchaseOrder> findById(Long purchaseOrderId);

    List<PurchaseOrderLine> findLinesByPurchaseOrderId(Long purchaseOrderId);

    boolean existsByPurchaseOrderNo(String purchaseOrderNo);

    /** 해당 접두사(예: PO-20261002-)로 시작하는 발주 번호 개수. 일련번호 채번에 쓴다. */
    long countByPurchaseOrderNoPrefix(String prefix);

    /** 공급처에 진행 중인 발주(REQUESTED·CONFIRMED)가 있는지. 공급처 비활성화 가드(SUPPLIER_IN_USE)에 쓴다. */
    boolean existsInProgressBySupplierId(Long supplierId);
}
