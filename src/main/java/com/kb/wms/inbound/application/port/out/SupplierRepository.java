package com.kb.wms.inbound.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.domain.entity.Supplier;

/**
 * 공급처 영속성 아웃바운드 포트.
 */
public interface SupplierRepository {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long supplierId);

    /** 조건이 null이면 해당 조건은 무시한다. 공급처명 오름차순. */
    List<Supplier> search(SupplierSearchCondition condition);

    boolean existsById(Long supplierId);

    boolean existsBySupplierCode(String supplierCode);
}
