package com.kb.wms.warehouse.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.LockModeType;

public interface WarehouseJpaRepository extends JpaRepository<WarehouseJpaEntity, Long> {

    boolean existsByWarehouseCode(String warehouseCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WarehouseJpaEntity w where w.warehouseId = :warehouseId")
    Optional<WarehouseJpaEntity> findByIdForUpdate(@Param("warehouseId") Long warehouseId);

    @Query("""
            select w from WarehouseJpaEntity w
            where (:status is null or w.status = :status)
              and (:keyword is null
                   or lower(w.name) like lower(concat('%', :keyword, '%'))
                   or lower(w.warehouseCode) like lower(concat('%', :keyword, '%')))
            order by w.createdAt desc, w.warehouseId desc
            """)
    List<WarehouseJpaEntity> search(@Param("keyword") String keyword, @Param("status") WarehouseStatus status);
}
