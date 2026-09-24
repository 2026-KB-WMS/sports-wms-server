package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.warehouse.domain.entity.WarehouseSection;

/**
 * 창고 구역 영속성 아웃바운드 포트.
 */
public interface WarehouseSectionRepository {

    WarehouseSection save(WarehouseSection section);

    Optional<WarehouseSection> findById(Long sectionId);

    /**
     * SELECT ... FOR UPDATE로 조회한다. 사용 용량(current_capacity)을 바꿀 때 트랜잭션 안에서만 호출한다.
     */
    Optional<WarehouseSection> findByIdForUpdate(Long sectionId);

    /**
     * warehouseId가 null이면 조건을 무시하고 조회한다.
     */
    List<WarehouseSection> findAll(Long warehouseId);

    boolean existsByWarehouseIdAndSectionCode(Long warehouseId, String sectionCode);
}
