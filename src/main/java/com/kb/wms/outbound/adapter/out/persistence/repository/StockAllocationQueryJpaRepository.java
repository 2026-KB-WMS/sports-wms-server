package com.kb.wms.outbound.adapter.out.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.kb.wms.outbound.adapter.out.persistence.entity.StockAllocationJpaEntity;
import com.kb.wms.outbound.application.port.in.result.FefoStockCandidate;
import com.kb.wms.outbound.application.port.in.result.StockAllocationSummary;
import com.kb.wms.outbound.application.port.in.result.StockAllocationView;
import com.kb.wms.outbound.domain.enums.AllocationStatus;

/** 재고 할당 조회 전용. 다른 도메인 테이블은 ID 기준 읽기 전용 조인만 한다(ADR-007). */
public interface StockAllocationQueryJpaRepository extends Repository<StockAllocationJpaEntity, Long> {

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.StockAllocationSummary(
                a.allocationId, o.storeOrderId, o.orderNo, a.storeOrderLineId, o.warehouseId,
                k.skuId, k.skuCode, k.name, a.inventoryLotId, lot.lotId, lot.lotNumber, lot.expiryDate,
                sec.sectionId, sec.sectionCode, a.allocatedQuantity, a.pickedQuantity, a.status,
                a.allocatedAt, a.releasedAt)
            from StockAllocationJpaEntity a
            join StoreOrderLineJpaEntity sl on sl.storeOrderLineId = a.storeOrderLineId
            join StoreOrderJpaEntity o on o.storeOrderId = sl.storeOrderId
            join ProductSkuJpaEntity k on k.skuId = sl.skuId
            join InventoryLotJpaEntity il on il.inventoryLotId = a.inventoryLotId
            join LotJpaEntity lot on lot.lotId = il.lotId
            join WarehouseSectionJpaEntity sec on sec.sectionId = il.sectionId
            where (:storeOrderId is null or o.storeOrderId = :storeOrderId)
              and (:warehouseId is null or o.warehouseId = :warehouseId)
              and (:skuId is null or k.skuId = :skuId)
              and (:status is null or a.status = :status)
              and (:keyword is null
                   or lower(o.orderNo) like lower(concat('%', :keyword, '%'))
                   or lower(k.skuCode) like lower(concat('%', :keyword, '%'))
                   or lower(lot.lotNumber) like lower(concat('%', :keyword, '%')))
            order by a.allocatedAt desc, a.allocationId desc
            """)
    List<StockAllocationSummary> search(@Param("storeOrderId") Long storeOrderId,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("skuId") Long skuId,
                                        @Param("status") AllocationStatus status,
                                        @Param("keyword") String keyword);

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.StockAllocationSummary(
                a.allocationId, o.storeOrderId, o.orderNo, a.storeOrderLineId, o.warehouseId,
                k.skuId, k.skuCode, k.name, a.inventoryLotId, lot.lotId, lot.lotNumber, lot.expiryDate,
                sec.sectionId, sec.sectionCode, a.allocatedQuantity, a.pickedQuantity, a.status,
                a.allocatedAt, a.releasedAt)
            from StockAllocationJpaEntity a
            join StoreOrderLineJpaEntity sl on sl.storeOrderLineId = a.storeOrderLineId
            join StoreOrderJpaEntity o on o.storeOrderId = sl.storeOrderId
            join ProductSkuJpaEntity k on k.skuId = sl.skuId
            join InventoryLotJpaEntity il on il.inventoryLotId = a.inventoryLotId
            join LotJpaEntity lot on lot.lotId = il.lotId
            join WarehouseSectionJpaEntity sec on sec.sectionId = il.sectionId
            where a.allocationId in :allocationIds
            order by a.allocationId asc
            """)
    List<StockAllocationSummary> findSummariesByIds(@Param("allocationIds") Collection<Long> allocationIds);

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.StockAllocationView(
                a.allocationId, a.status, a.allocatedQuantity, a.pickedQuantity, a.allocatedAt, a.allocatedBy,
                a.releasedAt, o.storeOrderId, o.orderNo, o.storeId, st.name, o.warehouseId, a.storeOrderLineId,
                sl.requestedQuantity, k.skuId, k.skuCode, k.name, a.inventoryLotId, lot.lotId, lot.lotNumber,
                lot.expiryDate, sec.sectionId, sec.sectionCode, sec.name)
            from StockAllocationJpaEntity a
            join StoreOrderLineJpaEntity sl on sl.storeOrderLineId = a.storeOrderLineId
            join StoreOrderJpaEntity o on o.storeOrderId = sl.storeOrderId
            join StoreJpaEntity st on st.storeId = o.storeId
            join ProductSkuJpaEntity k on k.skuId = sl.skuId
            join InventoryLotJpaEntity il on il.inventoryLotId = a.inventoryLotId
            join LotJpaEntity lot on lot.lotId = il.lotId
            join WarehouseSectionJpaEntity sec on sec.sectionId = il.sectionId
            where a.allocationId = :allocationId
            """)
    Optional<StockAllocationView> findView(@Param("allocationId") Long allocationId);

    @Query("""
            select o.outboundId
            from OutboundLineJpaEntity l
            join OutboundJpaEntity o on o.outboundId = l.outboundId
            where l.allocationId = :allocationId
              and o.status <> com.kb.wms.outbound.domain.enums.OutboundStatus.CANCELED
            """)
    List<Long> findActiveOutboundIds(@Param("allocationId") Long allocationId);

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.FefoStockCandidate(
                lot.skuId, il.inventoryLotId, il.sectionId, lot.lotId, il.onHandQuantity - il.allocatedQuantity)
            from InventoryLotJpaEntity il
            join LotJpaEntity lot on lot.lotId = il.lotId
            join WarehouseSectionJpaEntity sec on sec.sectionId = il.sectionId
            where sec.warehouseId = :warehouseId
              and sec.status = com.kb.wms.warehouse.domain.enums.WarehouseStatus.ACTIVE
              and lot.skuId in :skuIds
              and lot.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
              and il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
              and il.onHandQuantity - il.allocatedQuantity > 0
            order by lot.skuId asc,
                     case when lot.expiryDate is null then 1 else 0 end asc,
                     lot.expiryDate asc, lot.createdAt asc, il.inventoryLotId asc
            """)
    List<FefoStockCandidate> findFefoCandidates(@Param("warehouseId") Long warehouseId,
                                                @Param("skuIds") Collection<Long> skuIds);
}
