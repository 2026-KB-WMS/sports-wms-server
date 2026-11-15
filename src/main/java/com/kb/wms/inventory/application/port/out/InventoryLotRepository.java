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
     * 재고 행의 구역 ID만 읽는다. 구역을 먼저 잠그려고 재고 행을 읽어야 할 때 쓴다.
     * 행 전체를 읽으면 그 객체가 영속성 컨텍스트에 남아, 뒤이은 ForUpdate 조회가 락만 잡고 낡은 값을 돌려준다.
     */
    Optional<Long> findSectionIdById(Long inventoryLotId);

    /**
     * 여러 행을 inventory_lot_id 오름차순으로 잠근다(교착 방지). 없는 ID는 결과에서 빠진다.
     */
    List<InventoryLot> findAllByIdsForUpdate(Collection<Long> inventoryLotIds);

    /**
     * UNIQUE(section_id, lot_id) 기준 조회 + 잠금 (입고 반영 시 기존 행 찾기).
     */
    Optional<InventoryLot> findBySectionIdAndLotIdForUpdate(Long sectionId, Long lotId);

    /**
     * 한 로트의 모든 재고 행을 inventory_lot_id 오름차순으로 잠근다. 로트 상태 변경 전 수량 확인에 쓴다.
     */
    List<InventoryLot> findAllByLotIdForUpdate(Long lotId);

    boolean existsById(Long inventoryLotId);
}
