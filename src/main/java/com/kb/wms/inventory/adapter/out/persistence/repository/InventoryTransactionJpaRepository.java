package com.kb.wms.inventory.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryTransactionJpaEntity;
import com.kb.wms.inventory.application.port.in.result.InventoryTransactionView;
import com.kb.wms.inventory.domain.enums.ReferenceType;
import com.kb.wms.inventory.domain.enums.TransactionType;

public interface InventoryTransactionJpaRepository extends JpaRepository<InventoryTransactionJpaEntity, Long> {

    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.InventoryTransactionView(
                t.transactionId, t.inventoryLotId, ws.warehouseId, ws.sectionId, ws.sectionCode,
                s.skuId, s.skuCode, l.lotId, l.lotNumber,
                t.transactionType, t.quantityDelta, t.beforeQuantity, t.afterQuantity,
                t.referenceType, t.referenceId, t.reason, t.createdBy, t.createdAt)
            from InventoryTransactionJpaEntity t
            join InventoryLotJpaEntity il on il.inventoryLotId = t.inventoryLotId
            join LotJpaEntity l on l.lotId = il.lotId
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where (:inventoryLotId is null or t.inventoryLotId = :inventoryLotId)
              and (:warehouseId is null or ws.warehouseId = :warehouseId)
              and (:sectionId is null or il.sectionId = :sectionId)
              and (:skuId is null or s.skuId = :skuId)
              and (:lotId is null or il.lotId = :lotId)
              and (:transactionType is null or t.transactionType = :transactionType)
              and (:referenceType is null or t.referenceType = :referenceType)
              and (:referenceId is null or t.referenceId = :referenceId)
              and (:createdFrom is null or t.createdAt >= :createdFrom)
              and (:createdTo is null or t.createdAt <= :createdTo)
            order by t.createdAt desc, t.transactionId desc
            """)
    List<InventoryTransactionView> findViews(@Param("inventoryLotId") Long inventoryLotId,
                                             @Param("warehouseId") Long warehouseId,
                                             @Param("sectionId") Long sectionId,
                                             @Param("skuId") Long skuId,
                                             @Param("lotId") Long lotId,
                                             @Param("transactionType") TransactionType transactionType,
                                             @Param("referenceType") ReferenceType referenceType,
                                             @Param("referenceId") Long referenceId,
                                             @Param("createdFrom") LocalDateTime createdFrom,
                                             @Param("createdTo") LocalDateTime createdTo);
}
