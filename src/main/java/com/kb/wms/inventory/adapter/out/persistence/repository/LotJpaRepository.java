package com.kb.wms.inventory.adapter.out.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inventory.adapter.out.persistence.entity.LotJpaEntity;
import com.kb.wms.inventory.application.port.in.result.LotInboundView;
import com.kb.wms.inventory.application.port.in.result.LotSummary;

public interface LotJpaRepository extends JpaRepository<LotJpaEntity, Long> {

    Optional<LotJpaEntity> findBySkuIdAndSupplierIdAndLotNumber(Long skuId, Long supplierId, String lotNumber);

    /**
     * 로트 + SKU + 공급처명 조회 (lotId가 있으면 단건 조회에도 사용).
     */
    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.LotSummary(
                l.lotId, l.lotNumber, s.skuId, s.skuCode, s.name, l.supplierId, su.name,
                l.manufacturedDate, l.expiryDate, l.status, l.unitCost, l.createdAt, l.updatedAt)
            from LotJpaEntity l
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            left join SupplierJpaEntity su on su.supplierId = l.supplierId
            where (:lotId is null or l.lotId = :lotId)
              and (:skuId is null or l.skuId = :skuId)
              and (:supplierId is null or l.supplierId = :supplierId)
              and (:expiringBefore is null or l.expiryDate <= :expiringBefore)
              and (:keyword is null or lower(l.lotNumber) like lower(concat('%', :keyword, '%')))
            order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate, l.lotId
            """)
    List<LotSummary> findSummaries(@Param("lotId") Long lotId,
                                   @Param("skuId") Long skuId,
                                   @Param("supplierId") Long supplierId,
                                   @Param("expiringBefore") LocalDate expiringBefore,
                                   @Param("keyword") String keyword);

    /**
     * 로트가 입고된 이력. InboundLine.lot_id가 이 로트인 항목을 입고 헤더와 조인한다(조회 전용, ADR-007).
     * 입고 완료(COMPLETED) 건만 대상으로 한다.
     */
    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.LotInboundView(
                i.inboundId, i.inboundNo, i.warehouseId, i.receivedAt,
                il.receivedQuantity, il.acceptedQuantity, il.defectiveQuantity, il.receivedUnitPrice)
            from InboundLineJpaEntity il
            join InboundJpaEntity i on i.inboundId = il.inboundId
            where il.lotId = :lotId
              and i.status = com.kb.wms.inbound.domain.enums.InboundStatus.COMPLETED
            order by i.receivedAt desc, i.inboundId desc, il.inboundLineId
            """)
    List<LotInboundView> findInbounds(@Param("lotId") Long lotId);
}
