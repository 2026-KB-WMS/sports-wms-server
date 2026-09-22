package com.kb.wms.warehouse.adapter.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;

public interface WarehouseSectionJpaRepository extends JpaRepository<WarehouseSectionJpaEntity, Long> {

    boolean existsByWarehouseIdAndSectionCode(Long warehouseId, String sectionCode);

    @Query("""
            select s from WarehouseSectionJpaEntity s
            where (:warehouseId is null or s.warehouseId = :warehouseId)
            """)
    List<WarehouseSectionJpaEntity> findAllByFilter(@Param("warehouseId") Long warehouseId);
}
