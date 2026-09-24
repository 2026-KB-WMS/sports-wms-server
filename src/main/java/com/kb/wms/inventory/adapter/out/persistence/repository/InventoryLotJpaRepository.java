package com.kb.wms.inventory.adapter.out.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.inventory.adapter.out.persistence.entity.InventoryLotJpaEntity;
import com.kb.wms.inventory.application.port.in.result.InventoryDetail;
import com.kb.wms.inventory.application.port.in.result.InventoryLotView;
import com.kb.wms.inventory.application.port.in.result.InventorySkuSummary;
import com.kb.wms.inventory.application.port.in.result.LowStockItem;
import com.kb.wms.inventory.domain.enums.QualityStatus;

import jakarta.persistence.LockModeType;

/**
 * 가용 수량 기준(여러 쿼리 공통): 재고 품질 상태와 로트 상태가 모두 AVAILABLE인 행의 on_hand - allocated.
 */
public interface InventoryLotJpaRepository extends JpaRepository<InventoryLotJpaEntity, Long> {

    // ---------- 잠금 조회 (SELECT ... FOR UPDATE) ----------

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select il from InventoryLotJpaEntity il where il.inventoryLotId = :id")
    Optional<InventoryLotJpaEntity> findByIdForUpdate(@Param("id") Long inventoryLotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select il from InventoryLotJpaEntity il
            where il.inventoryLotId in :ids
            order by il.inventoryLotId
            """)
    List<InventoryLotJpaEntity> findAllByIdInForUpdate(@Param("ids") Collection<Long> inventoryLotIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select il from InventoryLotJpaEntity il
            where il.sectionId = :sectionId and il.lotId = :lotId
            """)
    Optional<InventoryLotJpaEntity> findBySectionIdAndLotIdForUpdate(@Param("sectionId") Long sectionId,
                                                                     @Param("lotId") Long lotId);

    // ---------- 조회 전용 (다른 도메인 테이블은 ID 조인으로 읽기만) ----------

    @Query("select count(s) > 0 from ProductSkuJpaEntity s where s.skuId = :skuId")
    boolean existsSkuId(@Param("skuId") Long skuId);

    @Query("select count(w) > 0 from WarehouseJpaEntity w where w.warehouseId = :warehouseId")
    boolean existsWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("select count(ws) > 0 from WarehouseSectionJpaEntity ws where ws.sectionId = :sectionId")
    boolean existsSectionId(@Param("sectionId") Long sectionId);

    @Query("""
            select count(il) > 0 from InventoryLotJpaEntity il
            where il.sectionId = :sectionId
              and (il.onHandQuantity > 0 or il.allocatedQuantity > 0)
            """)
    boolean existsStockInSection(@Param("sectionId") Long sectionId);

    @Query("""
            select count(il) > 0 from InventoryLotJpaEntity il
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where ws.warehouseId = :warehouseId
              and (il.onHandQuantity > 0 or il.allocatedQuantity > 0)
            """)
    boolean existsStockInWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.InventorySkuSummary(
                s.skuId, s.skuCode, s.name, s.unit,
                sum(il.onHandQuantity),
                sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                          and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                         then il.onHandQuantity - il.allocatedQuantity else 0L end),
                sum(il.allocatedQuantity),
                sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.DEFECTIVE
                         then il.onHandQuantity else 0L end))
            from InventoryLotJpaEntity il
            join LotJpaEntity l on l.lotId = il.lotId
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where (:skuId is null or s.skuId = :skuId)
              and (:warehouseId is null or ws.warehouseId = :warehouseId)
              and (:keyword is null
                   or lower(s.skuCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.name) like lower(concat('%', :keyword, '%')))
            group by s.skuId, s.skuCode, s.name, s.unit
            order by s.skuCode
            """)
    List<InventorySkuSummary> findSkuSummaries(@Param("skuId") Long skuId,
                                               @Param("warehouseId") Long warehouseId,
                                               @Param("keyword") String keyword);

    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.InventoryLotView(
                il.inventoryLotId, l.lotId, l.lotNumber, s.skuId, s.skuCode, s.name,
                ws.warehouseId, ws.sectionId, ws.sectionCode, ws.name,
                il.onHandQuantity, il.allocatedQuantity,
                case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                      and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                     then il.onHandQuantity - il.allocatedQuantity else 0L end,
                il.qualityStatus, l.expiryDate, il.lastCountedAt)
            from InventoryLotJpaEntity il
            join LotJpaEntity l on l.lotId = il.lotId
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where (:skuId is null or s.skuId = :skuId)
              and (:warehouseId is null or ws.warehouseId = :warehouseId)
              and (:sectionId is null or il.sectionId = :sectionId)
              and (:lotId is null or il.lotId = :lotId)
              and (:expiringBefore is null or l.expiryDate <= :expiringBefore)
              and (:qualityStatus is null or il.qualityStatus = :qualityStatus)
              and (:includeEmpty = true or il.onHandQuantity > 0)
            order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate, il.inventoryLotId
            """)
    List<InventoryLotView> findLotViews(@Param("skuId") Long skuId,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("sectionId") Long sectionId,
                                        @Param("lotId") Long lotId,
                                        @Param("expiringBefore") LocalDate expiringBefore,
                                        @Param("qualityStatus") QualityStatus qualityStatus,
                                        @Param("includeEmpty") boolean includeEmpty);

    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.InventoryDetail(
                il.inventoryLotId, w.warehouseId, w.name, ws.sectionId, ws.sectionCode, ws.name,
                s.skuId, s.skuCode, s.name, s.unit,
                l.lotId, l.lotNumber, l.supplierId, l.manufacturedDate, l.expiryDate, l.status, l.unitCost,
                il.onHandQuantity, il.allocatedQuantity,
                case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                      and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                     then il.onHandQuantity - il.allocatedQuantity else 0L end,
                il.qualityStatus, il.lastCountedAt, il.createdAt, il.updatedAt)
            from InventoryLotJpaEntity il
            join LotJpaEntity l on l.lotId = il.lotId
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            join WarehouseJpaEntity w on w.warehouseId = ws.warehouseId
            where il.inventoryLotId = :id
            """)
    Optional<InventoryDetail> findDetail(@Param("id") Long inventoryLotId);

    /**
     * SKU 기준으로 left join해 재고가 없는 SKU도 가용 0으로 포함한다.
     * 창고 필터는 가용 합계(CASE) 안에서 적용해, 다른 창고 재고만 있는 SKU도 가용 0으로 남긴다.
     */
    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.LowStockItem(
                s.skuId, s.skuCode, s.name, s.unit, s.safetyStockQuantity,
                coalesce(sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                                   and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                                   and (:warehouseId is null or ws.warehouseId = :warehouseId)
                                  then il.onHandQuantity - il.allocatedQuantity else 0L end), 0L))
            from ProductSkuJpaEntity s
            left join LotJpaEntity l on l.skuId = s.skuId
            left join InventoryLotJpaEntity il on il.lotId = l.lotId
            left join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where s.status = com.kb.wms.product.domain.enums.ProductStatus.ACTIVE
              and s.safetyStockQuantity > 0
              and (:keyword is null
                   or lower(s.skuCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.name) like lower(concat('%', :keyword, '%')))
            group by s.skuId, s.skuCode, s.name, s.unit, s.safetyStockQuantity
            having coalesce(sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                                      and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                                      and (:warehouseId is null or ws.warehouseId = :warehouseId)
                                     then il.onHandQuantity - il.allocatedQuantity else 0L end), 0L)
                   <= s.safetyStockQuantity
            order by s.safetyStockQuantity
                     - coalesce(sum(case when il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
                                          and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
                                          and (:warehouseId is null or ws.warehouseId = :warehouseId)
                                         then il.onHandQuantity - il.allocatedQuantity else 0L end), 0L) desc,
                     s.skuCode
            """)
    List<LowStockItem> findLowStock(@Param("warehouseId") Long warehouseId,
                                    @Param("keyword") String keyword);

    @Query("""
            select new com.kb.wms.inventory.application.port.in.result.InventoryLotView(
                il.inventoryLotId, l.lotId, l.lotNumber, s.skuId, s.skuCode, s.name,
                ws.warehouseId, ws.sectionId, ws.sectionCode, ws.name,
                il.onHandQuantity, il.allocatedQuantity, il.onHandQuantity - il.allocatedQuantity,
                il.qualityStatus, l.expiryDate, il.lastCountedAt)
            from InventoryLotJpaEntity il
            join LotJpaEntity l on l.lotId = il.lotId
            join ProductSkuJpaEntity s on s.skuId = l.skuId
            join WarehouseSectionJpaEntity ws on ws.sectionId = il.sectionId
            where ws.warehouseId = :warehouseId
              and l.skuId = :skuId
              and il.qualityStatus = com.kb.wms.inventory.domain.enums.QualityStatus.AVAILABLE
              and l.status = com.kb.wms.inventory.domain.enums.LotStatus.AVAILABLE
              and ws.status = com.kb.wms.warehouse.domain.enums.WarehouseStatus.ACTIVE
              and il.onHandQuantity > il.allocatedQuantity
            order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate, l.createdAt, il.inventoryLotId
            """)
    List<InventoryLotView> findAllocatableStocks(@Param("warehouseId") Long warehouseId,
                                                 @Param("skuId") Long skuId);
}
