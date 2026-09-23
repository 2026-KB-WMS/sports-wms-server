package com.kb.wms.warehouse.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseMemberJpaEntity;

public interface WarehouseMemberJpaRepository extends JpaRepository<WarehouseMemberJpaEntity, Long> {

    boolean existsByWarehouseIdAndUserId(Long warehouseId, Long userId);

    List<WarehouseMemberJpaEntity> findByUserId(Long userId);

    @Query("""
            select m from WarehouseMemberJpaEntity m
            where (:warehouseId is null or m.warehouseId = :warehouseId)
            and (:userId is null or m.userId = :userId)
            """)
    List<WarehouseMemberJpaEntity> findAllByFilter(@Param("warehouseId") Long warehouseId, @Param("userId") Long userId);
}
