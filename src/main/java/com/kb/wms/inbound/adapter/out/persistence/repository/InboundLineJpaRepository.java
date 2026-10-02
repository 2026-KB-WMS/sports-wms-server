package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundLineJpaEntity;
import com.kb.wms.inbound.application.port.in.result.InboundLineView;

public interface InboundLineJpaRepository extends JpaRepository<InboundLineJpaEntity, Long> {

    List<InboundLineJpaEntity> findByInboundIdOrderByInboundLineIdAsc(Long inboundId);

    /** 검수 항목 교체 시 새 항목과 유니크 키가 겹치지 않도록 즉시 삭제하고 영속성 컨텍스트를 비운다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InboundLineJpaEntity l where l.inboundId = :inboundId")
    void deleteByInboundId(@Param("inboundId") Long inboundId);

    /**
     * 검수 항목에 발주 항목(단가 스냅샷·SKU), 로트, 구역 정보를 붙여 조회한다.
     * 구역은 아직 정해지지 않았을 수 있어 left join 한다.
     */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.InboundLineView(
                l.inboundLineId, l.purchaseOrderLineId, s.skuId, s.skuCode, s.name,
                lot.lotId, lot.lotNumber, lot.manufacturedDate, lot.expiryDate,
                l.receivedQuantity, l.acceptedQuantity, l.defectiveQuantity,
                l.acceptedSectionId, asec.sectionCode, l.defectSectionId, dsec.sectionCode,
                pol.orderedUnitPrice, l.receivedUnitPrice, l.lineAmount,
                l.priceChangeReason, l.inspectionNote, l.receivedAt, l.receivedBy)
            from InboundLineJpaEntity l
            join PurchaseOrderLineJpaEntity pol on pol.purchaseOrderLineId = l.purchaseOrderLineId
            join ProductSkuJpaEntity s on s.skuId = pol.skuId
            join LotJpaEntity lot on lot.lotId = l.lotId
            left join WarehouseSectionJpaEntity asec on asec.sectionId = l.acceptedSectionId
            left join WarehouseSectionJpaEntity dsec on dsec.sectionId = l.defectSectionId
            where l.inboundId = :inboundId
            order by s.skuCode, lot.lotNumber, l.inboundLineId
            """)
    List<InboundLineView> findLineViews(@Param("inboundId") Long inboundId);
}
