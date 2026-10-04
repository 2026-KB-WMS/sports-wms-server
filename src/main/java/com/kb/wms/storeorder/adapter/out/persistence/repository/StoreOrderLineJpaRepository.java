package com.kb.wms.storeorder.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderLineJpaEntity;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView;

import jakarta.persistence.LockModeType;

public interface StoreOrderLineJpaRepository extends JpaRepository<StoreOrderLineJpaEntity, Long> {

    List<StoreOrderLineJpaEntity> findByStoreOrderIdOrderByStoreOrderLineIdAsc(Long storeOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from StoreOrderLineJpaEntity l
            where l.storeOrderId = :storeOrderId
            order by l.storeOrderLineId asc
            """)
    List<StoreOrderLineJpaEntity> findByStoreOrderIdForUpdate(@Param("storeOrderId") Long storeOrderId);

    /** 조회 전용: SKU 코드·이름·단위를 product_sku에서 ID로 조인해 가져온다(ADR-007). */
    @Query("""
            select new com.kb.wms.storeorder.application.port.in.result.StoreOrderLineView(
                l.storeOrderLineId, l.skuId, k.skuCode, k.name, k.unit,
                l.requestedQuantity, l.allocatedQuantity, l.shippedQuantity,
                l.requestedUnitSupplyPrice, l.status)
            from StoreOrderLineJpaEntity l
            join ProductSkuJpaEntity k on k.skuId = l.skuId
            where l.storeOrderId = :storeOrderId
            order by k.skuCode asc, l.storeOrderLineId asc
            """)
    List<StoreOrderLineView> findLineViews(@Param("storeOrderId") Long storeOrderId);
}
