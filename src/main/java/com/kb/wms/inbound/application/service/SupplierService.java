package com.kb.wms.inbound.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;
import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.exception.SupplierErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService implements SupplierUseCase {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public Supplier registerSupplier(SupplierRegisterCommand command) {
        if (supplierRepository.existsBySupplierCode(command.supplierCode())) {
            throw new BusinessException(SupplierErrorCode.DUPLICATE_SUPPLIER_CODE);
        }
        Supplier supplier = Supplier.register(
                command.supplierCode(), command.name(), command.managerName(),
                command.contactNumber(), command.email(), command.address());
        return supplierRepository.save(supplier);
    }

    /**
     * 창고 관리자에게는 활성 공급처만 노출하는 규칙은 역할(인증) 정보가 필요하므로
     * 인증 도메인 연동 시 웹 어댑터에서 isActive=true로 강제해 넘긴다.
     */
    @Override
    public List<Supplier> getSuppliers(SupplierSearchCondition condition) {
        return supplierRepository.search(condition);
    }

    @Override
    public Supplier getSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
    }

    @Override
    @Transactional
    public Supplier updateSupplier(Long supplierId, SupplierUpdateCommand command) {
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

        if (command.name() != null) {
            supplier.changeName(command.name());
        }
        if (command.managerName() != null) {
            supplier.changeManagerName(command.managerName());
        }
        if (command.contactNumber() != null) {
            supplier.changeContactNumber(command.contactNumber());
        }
        if (command.email() != null) {
            supplier.changeEmail(command.email());
        } else if (command.clearEmail()) {
            supplier.changeEmail(null);
        }
        if (command.address() != null) {
            supplier.changeAddress(command.address());
        } else if (command.clearAddress()) {
            supplier.changeAddress(null);
        }

        return supplierRepository.save(supplier);
    }

    /**
     * 진행 중인 발주(REQUESTED·CONFIRMED) 검사(409 SUPPLIER_IN_USE)는
     * 발주(PurchaseOrder) 도메인이 구현되면 추가한다.
     */
    @Override
    @Transactional
    public Supplier deactivateSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
        if (!supplier.isActive()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 공급처입니다.");
        }
        supplier.deactivate();
        return supplierRepository.save(supplier);
    }
}
