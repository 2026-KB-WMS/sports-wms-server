package com.kb.wms.warehouse.application.port.out;

import java.math.BigDecimal;
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

    /**
     * 같은 상위 구역 아래 활성 직속 구역들의 수용량 합. parentSectionId가 null이면 창고의 최상위 활성 구역 합.
     * excludeSectionId가 있으면 그 구역은 합계에서 뺀다(자기 수용량을 바꿀 때).
     */
    BigDecimal sumActiveCapacity(Long warehouseId, Long parentSectionId, Long excludeSectionId);

    /** 활성 상태의 직속 하위 구역이 있는지 */
    boolean existsActiveChild(Long parentSectionId);

    /** 상태와 무관하게 직속 하위 구역이 하나라도 있는지(삭제 가능 여부 판단) */
    boolean existsChild(Long parentSectionId);

    void deleteById(Long sectionId);
}
