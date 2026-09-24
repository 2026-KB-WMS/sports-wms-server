package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.warehouse.application.port.in.query.WarehouseSearchCondition;
import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * 창고 영속성 아웃바운드 포트.
 */
public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(Long warehouseId);

    /** 조건이 null이면 해당 조건은 무시한다. 생성 일시 내림차순. */
    List<Warehouse> search(WarehouseSearchCondition condition);

    boolean existsById(Long warehouseId);

    boolean existsByWarehouseCode(String warehouseCode);
}
