package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.kb.wms.inbound.adapter.out.persistence.entity.PurchaseOrderJpaEntity;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary;
import com.kb.wms.inbound.application.port.in.result.PurchaseOrderView;
import com.kb.wms.inbound.domain.enums.PurchaseOrderStatus;

public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderJpaEntity, Long> {

    boolean existsByPurchaseOrderNo(String purchaseOrderNo);

    long countByPurchaseOrderNoStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PurchaseOrderJpaEntity po where po.purchaseOrderId = :purchaseOrderId")
    Optional<PurchaseOrderJpaEntity> findByIdForUpdate(@Param("purchaseOrderId") Long purchaseOrderId);

    /** 진행 중 발주 = REQUESTED·CONFIRMED */
    @Query("""
            select count(po) > 0 from PurchaseOrderJpaEntity po
            where po.supplierId = :supplierId
              and po.status in (com.kb.wms.inbound.domain.enums.PurchaseOrderStatus.REQUESTED,
                                com.kb.wms.inbound.domain.enums.PurchaseOrderStatus.CONFIRMED)
            """)
    boolean existsInProgressBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * 발주는 항목이 1개 이상이므로 항목과 inner join해 항목 수·금액 합계를 집계한다.
     */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.PurchaseOrderSummary(
                po.purchaseOrderId, po.purchaseOrderNo, po.warehouseId, w.name, po.supplierId, s.name,
                po.status, po.expectedAt, count(l), sum(l.lineAmount), po.createdBy, cu.name, po.createdAt)
            from PurchaseOrderJpaEntity po
            join WarehouseJpaEntity w on w.warehouseId = po.warehouseId
            join SupplierJpaEntity s on s.supplierId = po.supplierId
            join PurchaseOrderLineJpaEntity l on l.purchaseOrderId = po.purchaseOrderId
            left join UserJpaEntity cu on cu.userId = po.createdBy
            where (:status is null or po.status = :status)
              and (:warehouseId is null or po.warehouseId = :warehouseId)
              and (:supplierId is null or po.supplierId = :supplierId)
              and (:keyword is null or lower(po.purchaseOrderNo) like lower(concat('%', :keyword, '%')))
              and (:createdFrom is null or po.createdAt >= :createdFrom)
              and (:createdTo is null or po.createdAt <= :createdTo)
            group by po.purchaseOrderId, po.purchaseOrderNo, po.warehouseId, w.name, po.supplierId, s.name,
                     po.status, po.expectedAt, po.createdBy, cu.name, po.createdAt
            order by po.createdAt desc, po.purchaseOrderId desc
            """)
    List<PurchaseOrderSummary> search(@Param("status") PurchaseOrderStatus status,
                                      @Param("warehouseId") Long warehouseId,
                                      @Param("supplierId") Long supplierId,
                                      @Param("keyword") String keyword,
                                      @Param("createdFrom") LocalDateTime createdFrom,
                                      @Param("createdTo") LocalDateTime createdTo);

    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.PurchaseOrderView(
                po.purchaseOrderId, po.purchaseOrderNo, po.warehouseId, w.name, po.supplierId, s.name,
                po.status, po.expectedAt, po.note, count(l), sum(l.lineAmount), po.createdBy, cu.name,
                po.createdAt, po.updatedAt)
            from PurchaseOrderJpaEntity po
            join WarehouseJpaEntity w on w.warehouseId = po.warehouseId
            join SupplierJpaEntity s on s.supplierId = po.supplierId
            join PurchaseOrderLineJpaEntity l on l.purchaseOrderId = po.purchaseOrderId
            left join UserJpaEntity cu on cu.userId = po.createdBy
            where po.purchaseOrderId = :purchaseOrderId
            group by po.purchaseOrderId, po.purchaseOrderNo, po.warehouseId, w.name, po.supplierId, s.name,
                     po.status, po.expectedAt, po.note, po.createdBy, cu.name, po.createdAt, po.updatedAt
            """)
    Optional<PurchaseOrderView> findView(@Param("purchaseOrderId") Long purchaseOrderId);
}
