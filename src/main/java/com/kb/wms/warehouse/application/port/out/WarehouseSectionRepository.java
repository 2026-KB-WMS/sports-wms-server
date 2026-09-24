package com.kb.wms.warehouse.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.warehouse.application.port.in.query.WarehouseSectionSearchCondition;
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
     * 조건이 null이면 해당 조건은 무시하고 조회한다. 구역 코드 오름차순.
     */
    List<WarehouseSection> search(WarehouseSectionSearchCondition condition);

    /** 재고 잠금 대상 등 창고의 구역 전체 (필터 없음) */
    List<WarehouseSection> findAllByWarehouseId(Long warehouseId);

    boolean existsByWarehouseIdAndSectionCode(Long warehouseId, String sectionCode);

    /** 활성 상태의 직속 하위 구역이 있는지 */
    boolean existsActiveChild(Long parentSectionId);
}
