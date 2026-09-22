package com.kb.wms.warehouse.adapter.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseJpaEntity;

public interface WarehouseJpaRepository extends JpaRepository<WarehouseJpaEntity, Long> {

    boolean existsByWarehouseCode(String warehouseCode);
}
