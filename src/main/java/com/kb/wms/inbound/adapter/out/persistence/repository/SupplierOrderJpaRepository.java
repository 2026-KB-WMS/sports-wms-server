package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.SupplierOrderJpaEntity;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary;
import com.kb.wms.inbound.application.port.in.result.SupplierOrderView;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

import jakarta.persistence.LockModeType;

public interface SupplierOrderJpaRepository extends JpaRepository<SupplierOrderJpaEntity, Long> {

    boolean existsBySupplierOrderNo(String supplierOrderNo);

    long countBySupplierOrderNoStartingWith(String prefix);

    List<SupplierOrderJpaEntity> findByPurchaseOrderIdOrderBySupplierOrderIdAsc(Long purchaseOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select so from SupplierOrderJpaEntity so where so.supplierOrderId = :supplierOrderId")
    Optional<SupplierOrderJpaEntity> findByIdForUpdate(@Param("supplierOrderId") Long supplierOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select so from SupplierOrderJpaEntity so
            where so.purchaseOrderId = :purchaseOrderId
            order by so.supplierOrderId
            """)
    List<SupplierOrderJpaEntity> findByPurchaseOrderIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);

    /** 진행 중 발주서 = ISSUED·PARTIALLY_RECEIVED */
    @Query("""
            select count(so) > 0 from SupplierOrderJpaEntity so
            where so.supplierId = :supplierId
              and so.status in (com.kb.wms.inbound.domain.enums.SupplierOrderStatus.ISSUED,
                                com.kb.wms.inbound.domain.enums.SupplierOrderStatus.PARTIALLY_RECEIVED)
            """)
    boolean existsInProgressBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 발주서는 항목이 1개 이상이므로 항목과 inner join해 항목 수·금액 합계를 집계한다.
     */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary(
                so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                so.supplierId, s.name, po.warehouseId, w.name, so.status, count(l), sum(l.lineAmount), so.createdAt)
            from SupplierOrderJpaEntity so
            join PurchaseOrderJpaEntity po on po.purchaseOrderId = so.purchaseOrderId
            join WarehouseJpaEntity w on w.warehouseId = po.warehouseId
            join SupplierJpaEntity s on s.supplierId = so.supplierId
            join PurchaseOrderLineJpaEntity l on l.supplierOrderId = so.supplierOrderId
            where (:status is null or so.status = :status)
              and (:warehouseId is null or po.warehouseId = :warehouseId)
              and (:scoped = false or po.warehouseId in :warehouseIds)
              and (:supplierId is null or so.supplierId = :supplierId)
              and (:purchaseOrderId is null or so.purchaseOrderId = :purchaseOrderId)
              and (:keyword is null
                   or lower(so.supplierOrderNo) like lower(concat('%', :keyword, '%'))
                   or lower(po.purchaseOrderNo) like lower(concat('%', :keyword, '%')))
              and (:createdFrom is null or so.createdAt >= :createdFrom)
              and (:createdTo is null or so.createdAt <= :createdTo)
            group by so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                     so.supplierId, s.name, po.warehouseId, w.name, so.status, so.createdAt
            order by so.createdAt desc, so.supplierOrderId desc
            """)
    List<SupplierOrderSummary> search(@Param("status") SupplierOrderStatus status,
                                      @Param("warehouseId") Long warehouseId,
                                      @Param("supplierId") Long supplierId,
                                      @Param("purchaseOrderId") Long purchaseOrderId,
                                      @Param("keyword") String keyword,
                                      @Param("createdFrom") LocalDateTime createdFrom,
                                      @Param("createdTo") LocalDateTime createdTo,
                                      @Param("scoped") boolean scoped,
                                      @Param("warehouseIds") Collection<Long> warehouseIds);

    /** 요청 상세·확정 응답용. 발주서 ID 오름차순. */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SupplierOrderSummary(
                so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                so.supplierId, s.name, po.warehouseId, w.name, so.status, count(l), sum(l.lineAmount), so.createdAt)
            from SupplierOrderJpaEntity so
            join PurchaseOrderJpaEntity po on po.purchaseOrderId = so.purchaseOrderId
            join WarehouseJpaEntity w on w.warehouseId = po.warehouseId
            join SupplierJpaEntity s on s.supplierId = so.supplierId
            join PurchaseOrderLineJpaEntity l on l.supplierOrderId = so.supplierOrderId
            where so.purchaseOrderId = :purchaseOrderId
            group by so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                     so.supplierId, s.name, po.warehouseId, w.name, so.status, so.createdAt
            order by so.supplierOrderId
            """)
    List<SupplierOrderSummary> findSummariesByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SupplierOrderView(
                so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                so.supplierId, s.name, po.warehouseId, w.name, so.status, count(l), sum(l.lineAmount),
                so.createdAt, so.updatedAt)
            from SupplierOrderJpaEntity so
            join PurchaseOrderJpaEntity po on po.purchaseOrderId = so.purchaseOrderId
            join WarehouseJpaEntity w on w.warehouseId = po.warehouseId
            join SupplierJpaEntity s on s.supplierId = so.supplierId
            join PurchaseOrderLineJpaEntity l on l.supplierOrderId = so.supplierOrderId
            where so.supplierOrderId = :supplierOrderId
            group by so.supplierOrderId, so.supplierOrderNo, so.purchaseOrderId, po.purchaseOrderNo,
                     so.supplierId, s.name, po.warehouseId, w.name, so.status, so.createdAt, so.updatedAt
            """)
    Optional<SupplierOrderView> findView(@Param("supplierOrderId") Long supplierOrderId);

    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SupplierOrderLineView(
                l.purchaseOrderLineId, sku.skuId, sku.skuCode, sku.name, sku.unit,
                l.expectedQuantity, l.receivedQuantity,
                case when l.expectedQuantity > l.receivedQuantity
                     then l.expectedQuantity - l.receivedQuantity else 0L end,
                l.orderedUnitPrice, l.lineAmount, l.status)
            from PurchaseOrderLineJpaEntity l
            join ProductSkuJpaEntity sku on sku.skuId = l.skuId
            where l.supplierOrderId = :supplierOrderId
            order by sku.skuCode, l.purchaseOrderLineId
            """)
    List<SupplierOrderLineView> findLineViews(@Param("supplierOrderId") Long supplierOrderId);
}
