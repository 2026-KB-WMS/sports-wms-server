package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * 창고 영속성 아웃바운드 포트.
 */
public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(Long warehouseId);

    List<Warehouse> findAll();

    boolean existsByWarehouseCode(String warehouseCode);
}
