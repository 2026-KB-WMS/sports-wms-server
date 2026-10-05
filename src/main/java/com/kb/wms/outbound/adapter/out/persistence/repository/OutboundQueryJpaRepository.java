package com.kb.wms.outbound.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.kb.wms.outbound.adapter.out.persistence.entity.OutboundJpaEntity;
import com.kb.wms.outbound.application.port.in.result.OutboundLineView;
import com.kb.wms.outbound.application.port.in.result.OutboundSummary;
import com.kb.wms.outbound.application.port.in.result.OutboundView;
import com.kb.wms.outbound.domain.enums.OutboundStatus;

/** 출고 조회 전용. 다른 도메인 테이블은 ID 기준 읽기 전용 조인만 한다(ADR-007). */
public interface OutboundQueryJpaRepository extends Repository<OutboundJpaEntity, Long> {

    /** 출고에는 항목이 1개 이상이므로 항목과 inner join해 항목 수를 센다. 창고는 발주에 배정된 창고다. */
    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.OutboundSummary(
                ob.outboundId, ob.outboundNo, o.storeOrderId, o.orderNo, o.storeId, st.name,
                o.warehouseId, w.name, ob.status, count(l), ob.shippedAt, ob.shippedBy, ob.createdAt)
            from OutboundJpaEntity ob
            join StoreOrderJpaEntity o on o.storeOrderId = ob.storeOrderId
            join StoreJpaEntity st on st.storeId = o.storeId
            left join WarehouseJpaEntity w on w.warehouseId = o.warehouseId
            join OutboundLineJpaEntity l on l.outboundId = ob.outboundId
            where (:status is null or ob.status = :status)
              and (:warehouseId is null or o.warehouseId = :warehouseId)
              and (:storeId is null or o.storeId = :storeId)
              and (:storeOrderId is null or o.storeOrderId = :storeOrderId)
              and (:keyword is null
                   or lower(ob.outboundNo) like lower(concat('%', :keyword, '%'))
                   or lower(o.orderNo) like lower(concat('%', :keyword, '%')))
              and (:createdFrom is null or ob.createdAt >= :createdFrom)
              and (:createdTo is null or ob.createdAt <= :createdTo)
            group by ob.outboundId, ob.outboundNo, o.storeOrderId, o.orderNo, o.storeId, st.name,
                     o.warehouseId, w.name, ob.status, ob.shippedAt, ob.shippedBy, ob.createdAt
            order by ob.createdAt desc, ob.outboundId desc
            """)
    List<OutboundSummary> search(@Param("status") OutboundStatus status,
                                 @Param("warehouseId") Long warehouseId,
                                 @Param("storeId") Long storeId,
                                 @Param("storeOrderId") Long storeOrderId,
                                 @Param("keyword") String keyword,
                                 @Param("createdFrom") LocalDateTime createdFrom,
                                 @Param("createdTo") LocalDateTime createdTo);

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.OutboundView(
                ob.outboundId, ob.outboundNo, ob.status, o.storeOrderId, o.orderNo, o.storeId, st.name,
                o.warehouseId, w.name, ob.shippedAt, ob.shippedBy, ob.deliveredAt, ob.note,
                ob.createdAt, ob.updatedAt)
            from OutboundJpaEntity ob
            join StoreOrderJpaEntity o on o.storeOrderId = ob.storeOrderId
            join StoreJpaEntity st on st.storeId = o.storeId
            left join WarehouseJpaEntity w on w.warehouseId = o.warehouseId
            where ob.outboundId = :outboundId
            """)
    Optional<OutboundView> findView(@Param("outboundId") Long outboundId);

    @Query("""
            select new com.kb.wms.outbound.application.port.in.result.OutboundLineView(
                l.outboundLineId, a.allocationId, a.storeOrderLineId, k.skuId, k.skuCode, k.name, k.unit,
                a.inventoryLotId, lot.lotId, lot.lotNumber, lot.expiryDate, sec.sectionId, sec.sectionCode,
                a.allocatedQuantity, l.shippedQuantity, l.confirmedUnitSupplyPrice)
            from OutboundLineJpaEntity l
            join StockAllocationJpaEntity a on a.allocationId = l.allocationId
            join StoreOrderLineJpaEntity sl on sl.storeOrderLineId = a.storeOrderLineId
            join ProductSkuJpaEntity k on k.skuId = sl.skuId
            join InventoryLotJpaEntity il on il.inventoryLotId = a.inventoryLotId
            join LotJpaEntity lot on lot.lotId = il.lotId
            join WarehouseSectionJpaEntity sec on sec.sectionId = il.sectionId
            where l.outboundId = :outboundId
            order by l.outboundLineId asc
            """)
    List<OutboundLineView> findLineViews(@Param("outboundId") Long outboundId);
}
