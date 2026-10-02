package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderLineJpaEntity;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderLineView;

import jakarta.persistence.LockModeType;

public interface PurchaseOrderLineJpaRepository extends JpaRepository<PurchaseOrderLineJpaEntity, Long> {

    List<PurchaseOrderLineJpaEntity> findByPurchaseOrderIdOrderByPurchaseOrderLineIdAsc(Long purchaseOrderId);

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
                l.orderedUnitPrice, l.lineAmount, l.status)
            from PurchaseOrderLineJpaEntity l
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            where l.purchaseOrderId = :purchaseOrderId
            order by s.skuCode, l.purchaseOrderLineId
            """)
    List<PurchaseOrderLineView> findLineViews(@Param("purchaseOrderId") Long purchaseOrderId);
}
