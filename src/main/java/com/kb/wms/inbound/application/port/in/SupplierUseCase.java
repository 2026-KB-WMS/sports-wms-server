package com.kb.wms.inbound.application.port.in;

import java.util.List;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.domain.entity.Supplier;

/**
 * 공급처 등록/조회/수정/비활성화 유스케이스.
 * POST, GET, PATCH /api/v1/suppliers
 */
public interface SupplierUseCase {

    Supplier registerSupplier(SupplierRegisterCommand command);

    List<Supplier> getSuppliers(SupplierSearchCondition condition, AuthenticatedUser actor);

    Supplier getSupplier(Long supplierId, AuthenticatedUser actor);

    Supplier updateSupplier(Long supplierId, SupplierUpdateCommand command);

    Supplier deactivateSupplier(Long supplierId);
}
