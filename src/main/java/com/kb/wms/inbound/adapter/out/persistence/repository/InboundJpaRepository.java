package com.kb.wms.inbound.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inbound.adapter.out.persistence.entity.InboundJpaEntity;
import com.kb.wms.inbound.application.port.in.result.InboundSummary;
import com.kb.wms.inbound.application.port.in.result.InboundView;
import com.kb.wms.inbound.application.port.in.result.SectionCandidate;
import com.kb.wms.inbound.domain.enums.InboundStatus;

import jakarta.persistence.LockModeType;

public interface InboundJpaRepository extends JpaRepository<InboundJpaEntity, Long> {

    boolean existsByInboundNo(String inboundNo);

    long countByInboundNoStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InboundJpaEntity i where i.inboundId = :inboundId")
    Optional<InboundJpaEntity> findByIdForUpdate(@Param("inboundId") Long inboundId);

    /** 진행 중 입고 = ARRIVED·INSPECTING */
    @Query("""
            select count(i) > 0 from InboundJpaEntity i
            where i.purchaseOrderId = :purchaseOrderId
              and i.status in (com.kb.wms.inbound.domain.enums.InboundStatus.ARRIVED,
                               com.kb.wms.inbound.domain.enums.InboundStatus.INSPECTING)
            """)
    boolean existsInProgressByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

    @Query("""
            select count(i) > 0 from InboundJpaEntity i
            where i.purchaseOrderId = :purchaseOrderId
              and i.status <> com.kb.wms.inbound.domain.enums.InboundStatus.CANCELED
            """)
    boolean existsNotCanceledByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);

    /**
     * 입고에 발주(번호·공급처)·창고를 붙이고 검수 항목 수를 센다. 검수 전(ARRIVED)에는 항목이 없으므로
     * 항목은 left join 한다.
     */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.InboundSummary(
                i.inboundId, i.inboundNo, i.purchaseOrderId, po.purchaseOrderNo, po.supplierId, s.name,
                i.warehouseId, w.name, i.status, i.arrivedAt, i.receivedAt, i.receivedBy, count(l))
            from InboundJpaEntity i
            join PurchaseOrderJpaEntity po on po.purchaseOrderId = i.purchaseOrderId
            join SupplierJpaEntity s on s.supplierId = po.supplierId
            join WarehouseJpaEntity w on w.warehouseId = i.warehouseId
            left join InboundLineJpaEntity l on l.inboundId = i.inboundId
            where (:status is null or i.status = :status)
              and (:warehouseId is null or i.warehouseId = :warehouseId)
              and (:purchaseOrderId is null or i.purchaseOrderId = :purchaseOrderId)
              and (:keyword is null
                   or lower(i.inboundNo) like lower(concat('%', :keyword, '%'))
                   or lower(po.purchaseOrderNo) like lower(concat('%', :keyword, '%')))
              and (:arrivedFrom is null or i.arrivedAt >= :arrivedFrom)
              and (:arrivedTo is null or i.arrivedAt <= :arrivedTo)
            group by i.inboundId, i.inboundNo, i.purchaseOrderId, po.purchaseOrderNo, po.supplierId, s.name,
                     i.warehouseId, w.name, i.status, i.arrivedAt, i.receivedAt, i.receivedBy
            order by i.arrivedAt desc, i.inboundId desc
            """)
    List<InboundSummary> search(@Param("status") InboundStatus status,
                                @Param("warehouseId") Long warehouseId,
                                @Param("purchaseOrderId") Long purchaseOrderId,
                                @Param("keyword") String keyword,
                                @Param("arrivedFrom") LocalDateTime arrivedFrom,
                                @Param("arrivedTo") LocalDateTime arrivedTo);

    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.InboundView(
                i.inboundId, i.inboundNo, i.purchaseOrderId, po.purchaseOrderNo, po.status, po.supplierId, s.name,
                i.warehouseId, w.name, i.status, i.arrivedAt, i.receivedAt, i.receivedBy, ru.name, i.note, count(l),
                i.createdAt, i.updatedAt)
            from InboundJpaEntity i
            join PurchaseOrderJpaEntity po on po.purchaseOrderId = i.purchaseOrderId
            join SupplierJpaEntity s on s.supplierId = po.supplierId
            join WarehouseJpaEntity w on w.warehouseId = i.warehouseId
            left join InboundLineJpaEntity l on l.inboundId = i.inboundId
            left join UserJpaEntity ru on ru.userId = i.receivedBy
            where i.inboundId = :inboundId
            group by i.inboundId, i.inboundNo, i.purchaseOrderId, po.purchaseOrderNo, po.status, po.supplierId, s.name,
                     i.warehouseId, w.name, i.status, i.arrivedAt, i.receivedAt, i.receivedBy, ru.name, i.note,
                     i.createdAt, i.updatedAt
            """)
    Optional<InboundView> findView(@Param("inboundId") Long inboundId);

    /** 합격품을 둘 수 있는 구역: 활성, 불량 구역(DEFECT)이 아님, 가용 용량(capacity - currentCapacity) > 0 */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SectionCandidate(
                sec.sectionId, sec.parentSectionId, sec.sectionCode, sec.name, sec.sectionType,
                sec.capacity, sec.currentCapacity, sec.capacity - sec.currentCapacity)
            from WarehouseSectionJpaEntity sec
            where sec.warehouseId = :warehouseId
              and sec.status = com.kb.wms.warehouse.domain.enums.WarehouseStatus.ACTIVE
              and sec.sectionType <> 'DEFECT'
              and sec.capacity - sec.currentCapacity > 0
              and (:requiredQuantity is null or sec.capacity - sec.currentCapacity >= :requiredQuantity)
              and (:keyword is null
                   or lower(sec.name) like lower(concat('%', :keyword, '%'))
                   or lower(sec.sectionCode) like lower(concat('%', :keyword, '%')))
            order by sec.sectionCode
            """)
    List<SectionCandidate> findAssignableSections(@Param("warehouseId") Long warehouseId,
                                                  @Param("requiredQuantity") BigDecimal requiredQuantity,
                                                  @Param("keyword") String keyword);

    /** 불량품을 둘 수 있는 구역: 활성, 구역 유형 DEFECT, 가용 용량 > 0 */
    @Query("""
            select new com.kb.wms.inbound.application.port.in.result.SectionCandidate(
                sec.sectionId, sec.parentSectionId, sec.sectionCode, sec.name, sec.sectionType,
                sec.capacity, sec.currentCapacity, sec.capacity - sec.currentCapacity)
            from WarehouseSectionJpaEntity sec
            where sec.warehouseId = :warehouseId
              and sec.status = com.kb.wms.warehouse.domain.enums.WarehouseStatus.ACTIVE
              and sec.sectionType = 'DEFECT'
              and sec.capacity - sec.currentCapacity > 0
              and (:requiredQuantity is null or sec.capacity - sec.currentCapacity >= :requiredQuantity)
              and (:keyword is null
                   or lower(sec.name) like lower(concat('%', :keyword, '%'))
                   or lower(sec.sectionCode) like lower(concat('%', :keyword, '%')))
            order by sec.sectionCode
            """)
    List<SectionCandidate> findDefectSections(@Param("warehouseId") Long warehouseId,
                                              @Param("requiredQuantity") BigDecimal requiredQuantity,
                                              @Param("keyword") String keyword);
}
