package com.kb.wms.warehouse.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseSectionJpaEntity;
import com.kb.wms.warehouse.domain.enums.WarehouseStatus;

import jakarta.persistence.LockModeType;

public interface WarehouseSectionJpaRepository extends JpaRepository<WarehouseSectionJpaEntity, Long> {

    boolean existsByWarehouseIdAndSectionCode(Long warehouseId, String sectionCode);

    boolean existsByParentSectionIdAndStatus(Long parentSectionId, WarehouseStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WarehouseSectionJpaEntity s where s.sectionId = :sectionId")
    Optional<WarehouseSectionJpaEntity> findByIdForUpdate(@Param("sectionId") Long sectionId);

    @Query("""
            select s from WarehouseSectionJpaEntity s
            where (:warehouseId is null or s.warehouseId = :warehouseId)
            """)
    List<WarehouseSectionJpaEntity> findAllByFilter(@Param("warehouseId") Long warehouseId);
}
