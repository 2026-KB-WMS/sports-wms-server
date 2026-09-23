package com.kb.wms.inventory.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.kb.wms.inventory.domain.entity.InventoryLot;

/**
 * 재고 행(InventoryLot) 영속성 아웃바운드 포트.
 *
 * <p>ForUpdate 메서드는 SELECT ... FOR UPDATE(비관적 쓰기 락)로 조회하며 트랜잭션 안에서만 호출한다.
 * 수량을 바꾸는 흐름은 반드시 ForUpdate로 조회한 뒤 변경·저장한다.
 */
public interface InventoryLotRepository {

    InventoryLot save(InventoryLot inventoryLot);

    Optional<InventoryLot> findById(Long inventoryLotId);

    Optional<InventoryLot> findByIdForUpdate(Long inventoryLotId);

    /**
     * 여러 행을 inventory_lot_id 오름차순으로 잠근다(교착 방지). 없는 ID는 결과에서 빠진다.
     */
    List<InventoryLot> findAllByIdsForUpdate(Collection<Long> inventoryLotIds);

    /**
     * UNIQUE(section_id, lot_id) 기준 조회 + 잠금 (입고 반영 시 기존 행 찾기).
     */
    Optional<InventoryLot> findBySectionIdAndLotIdForUpdate(Long sectionId, Long lotId);

    boolean existsById(Long inventoryLotId);
}
