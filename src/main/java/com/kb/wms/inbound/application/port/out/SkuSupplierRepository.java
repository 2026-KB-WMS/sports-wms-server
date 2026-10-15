package com.kb.wms.inbound.application.port.out;

import java.util.Collection;
import java.util.List;

import com.kb.wms.inbound.domain.entity.SkuSupplier;

/**
 * SKU↔공급처 매핑 영속성 아웃바운드 포트. SKU는 ID로만 참조하며 상품 도메인 테이블을 읽지 않는다.
 */
public interface SkuSupplierRepository {

    SkuSupplier save(SkuSupplier skuSupplier);

    /** 매핑 ID 오름차순 */
    List<SkuSupplier> findBySkuId(Long skuId);

    /** 발주 확정 시 라인의 SKU들에 대해 지정 가능한 공급처를 한 번에 확인하려고 쓴다. */
    List<SkuSupplier> findBySkuIdIn(Collection<Long> skuIds);

    boolean existsBySkuIdAndSupplierId(Long skuId, Long supplierId);

    /** 매핑이 없으면 아무 일도 하지 않는다. 삭제한 건수를 돌려준다. */
    long deleteBySkuIdAndSupplierId(Long skuId, Long supplierId);
}
