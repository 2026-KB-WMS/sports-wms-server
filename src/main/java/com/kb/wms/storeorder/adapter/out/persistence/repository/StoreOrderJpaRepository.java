package com.kb.wms.storeorder.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.storeorder.adapter.out.persistence.entity.StoreOrderJpaEntity;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary;
import com.kb.wms.storeorder.application.port.in.result.StoreOrderView;
import com.kb.wms.storeorder.domain.enums.StoreOrderStatus;

import jakarta.persistence.LockModeType;

public interface StoreOrderJpaRepository extends JpaRepository<StoreOrderJpaEntity, Long> {

    boolean existsByOrderNo(String orderNo);

    long countByOrderNoStartingWith(String prefix);

    boolean existsByStoreIdAndStatusIn(Long storeId, Collection<StoreOrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from StoreOrderJpaEntity o where o.storeOrderId = :storeOrderId")
    Optional<StoreOrderJpaEntity> findByIdForUpdate(@Param("storeOrderId") Long storeOrderId);

    /**
     * 발주는 항목이 1개 이상이므로 항목과 inner join해 항목 수·금액 합계·부족 항목 수를 집계한다.
     * 창고는 배정 전에는 null이므로 left join이다. 금액은 저장하지 않고 요청 수량 × 공급 단가로 계산한다.
     */
    @Query("""
            select new com.kb.wms.storeorder.application.port.in.result.StoreOrderSummary(
                o.storeOrderId, o.orderNo, o.storeId, s.name, o.warehouseId, w.name,
                o.status, o.requestedAt, o.requestedDeliveryAt, count(l),
                sum(l.requestedQuantity * l.requestedUnitSupplyPrice),
                sum(case when l.shippedQuantity < l.requestedQuantity then 1L else 0L end))
            from StoreOrderJpaEntity o
            join StoreJpaEntity s on s.storeId = o.storeId
            left join WarehouseJpaEntity w on w.warehouseId = o.warehouseId
            join StoreOrderLineJpaEntity l on l.storeOrderId = o.storeOrderId
            where (:status is null or o.status = :status)
              and (:storeId is null or o.storeId = :storeId)
              and (:warehouseId is null or o.warehouseId = :warehouseId)
              and (:keyword is null
                   or lower(o.orderNo) like lower(concat('%', :keyword, '%'))
                   or lower(s.name) like lower(concat('%', :keyword, '%')))
              and (:requestedFrom is null or o.requestedAt >= :requestedFrom)
              and (:requestedTo is null or o.requestedAt <= :requestedTo)
            group by o.storeOrderId, o.orderNo, o.storeId, s.name, o.warehouseId, w.name,
                     o.status, o.requestedAt, o.requestedDeliveryAt
            order by o.requestedAt desc, o.storeOrderId desc
            """)
    List<StoreOrderSummary> search(@Param("status") StoreOrderStatus status,
                                   @Param("storeId") Long storeId,
                                   @Param("warehouseId") Long warehouseId,
                                   @Param("keyword") String keyword,
                                   @Param("requestedFrom") LocalDateTime requestedFrom,
                                   @Param("requestedTo") LocalDateTime requestedTo);

    @Query("""
            select new com.kb.wms.storeorder.application.port.in.result.StoreOrderView(
                o.storeOrderId, o.orderNo, o.storeId, s.name, o.warehouseId, w.name,
                o.status, o.requestedAt, o.requestedDeliveryAt, o.note, count(l),
                sum(l.requestedQuantity * l.requestedUnitSupplyPrice),
                sum(case when l.shippedQuantity < l.requestedQuantity then 1L else 0L end),
                o.createdBy, o.createdAt, o.updatedAt)
            from StoreOrderJpaEntity o
            join StoreJpaEntity s on s.storeId = o.storeId
            left join WarehouseJpaEntity w on w.warehouseId = o.warehouseId
            join StoreOrderLineJpaEntity l on l.storeOrderId = o.storeOrderId
            where o.storeOrderId = :storeOrderId
            group by o.storeOrderId, o.orderNo, o.storeId, s.name, o.warehouseId, w.name,
                     o.status, o.requestedAt, o.requestedDeliveryAt, o.note, o.createdBy,
                     o.createdAt, o.updatedAt
            """)
    Optional<StoreOrderView> findView(@Param("storeOrderId") Long storeOrderId);
}
