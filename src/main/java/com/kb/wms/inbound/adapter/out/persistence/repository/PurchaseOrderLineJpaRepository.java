package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderLineJpaEntity;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView;

import jakarta.persistence.LockModeType;

public interface PurchaseOrderLineJpaRepository extends JpaRepository<PurchaseOrderLineJpaEntity, Long> {

    List<PurchaseOrderLineJpaEntity> findByPurchaseOrderIdOrderByPurchaseOrderLineIdAsc(Long purchaseOrderId);

    List<PurchaseOrderLineJpaEntity> findBySupplierOrderIdOrderByPurchaseOrderLineIdAsc(Long supplierOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from PurchaseOrderLineJpaEntity l
            where l.supplierOrderId = :supplierOrderId
            order by l.purchaseOrderLineId
            """)
    List<PurchaseOrderLineJpaEntity> findBySupplierOrderIdForUpdate(@Param("supplierOrderId") Long supplierOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from PurchaseOrderLineJpaEntity l
            where l.purchaseOrderId = :purchaseOrderId
            order by l.purchaseOrderLineId
            """)
    List<PurchaseOrderLineJpaEntity> findByPurchaseOrderIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);

    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView(
                l.purchaseOrderLineId, s.skuId, s.skuCode, s.name, s.unit,
                l.expectedQuantity, l.receivedQuantity,
                case when l.expectedQuantity > l.receivedQuantity
                     then l.expectedQuantity - l.receivedQuantity else 0L end,
                l.orderedUnitPrice, l.lineAmount, l.status, l.supplierId, sup.name, l.supplierOrderId)
            from PurchaseOrderLineJpaEntity l
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            left join SupplierJpaEntity sup on sup.supplierId = l.supplierId
            where l.purchaseOrderId = :purchaseOrderId
            order by s.skuCode, l.purchaseOrderLineId
            """)
    List<PurchaseOrderLineView> findLineViews(@Param("purchaseOrderId") Long purchaseOrderId);

    /** 발주별로 라인에 배정된 공급처(중복 제거). 공급처가 배정되기 전 라인은 건너뛴다. */
    @Query("""
            select distinct new com.kb.wms.inbound.application.port.in.result.PurchaseOrderSupplierView(
                l.purchaseOrderId, sup.supplierId, sup.name)
            from PurchaseOrderLineJpaEntity l
            join SupplierJpaEntity sup on sup.supplierId = l.supplierId
            where l.purchaseOrderId in :purchaseOrderIds
            order by l.purchaseOrderId, sup.supplierId
            """)
    List<PurchaseOrderSupplierView> findSuppliers(@Param("purchaseOrderIds") Collection<Long> purchaseOrderIds);
}
