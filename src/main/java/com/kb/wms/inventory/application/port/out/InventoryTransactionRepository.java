package com.kb.wms.inventory.application.port.out;

import com.kb.wms.inventory.domain.entity.InventoryTransaction;

/**
 * 재고 이력 영속성 아웃바운드 포트. 이력은 추가만 하고 수정·삭제하지 않는다.
 */
public interface InventoryTransactionRepository {

    InventoryTransaction save(InventoryTransaction transaction);
}
