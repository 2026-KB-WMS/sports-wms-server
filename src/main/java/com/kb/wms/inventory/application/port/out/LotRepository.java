package com.kb.wms.inventory.application.port.out;

import java.util.Optional;

import com.kb.wms.inventory.domain.entity.Lot;

/**
 * 로트 영속성 아웃바운드 포트.
 */
public interface LotRepository {

    Lot save(Lot lot);

    Optional<Lot> findById(Long lotId);

    /** SELECT ... FOR UPDATE. 상태를 바꾸는 흐름에서만 트랜잭션 안에서 호출한다. */
    Optional<Lot> findByIdForUpdate(Long lotId);

    /**
     * UNIQUE(sku_id, supplier_id, lot_number) 기준 조회 (로트 find-or-create).
     */
    Optional<Lot> findBySkuIdAndSupplierIdAndLotNumber(Long skuId, Long supplierId, String lotNumber);

    boolean existsById(Long lotId);
}
