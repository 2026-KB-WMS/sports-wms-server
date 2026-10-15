package com.kb.wms.inbound.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inbound.application.port.in.SkuSupplierUseCase;
import com.kb.wms.inbound.application.port.in.result.SkuSupplierView;
import com.kb.wms.inbound.application.port.out.SkuLookupPort;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.SkuSupplier;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SkuSupplierErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * SKU↔공급처 매핑 관리(ADR-029, ADR-030). 역할은 SecurityConfig가 검사한다
 * (조회는 본사+창고 관리자, 등록·삭제는 본사).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkuSupplierService implements SkuSupplierUseCase {

    private final SkuSupplierRepository skuSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final SkuLookupPort skuLookupPort;

    @Override
    public List<SkuSupplierView> getSuppliers(Long skuId) {
        skuLookupPort.requireExists(skuId);
        return skuSupplierRepository.findBySkuId(skuId).stream()
                .map(mapping -> toView(mapping, requireSupplier(mapping.getSupplierId())))
                .toList();
    }

    @Override
    @Transactional
    public SkuSupplierView registerSupplier(Long skuId, Long supplierId) {
        skuLookupPort.requireExists(skuId);
        Supplier supplier = requireSupplier(supplierId);
        if (!supplier.isActive()) {
            throw new BusinessException(PurchaseOrderErrorCode.SUPPLIER_INACTIVE,
                    "비활성 공급처는 SKU에 새로 매핑할 수 없습니다. supplierId=" + supplierId);
        }
        if (skuSupplierRepository.existsBySkuIdAndSupplierId(skuId, supplierId)) {
            throw new BusinessException(SkuSupplierErrorCode.SKU_SUPPLIER_ALREADY_MAPPED);
        }
        SkuSupplier saved = skuSupplierRepository.save(SkuSupplier.register(skuId, supplierId));
        return toView(saved, supplier);
    }

    @Override
    @Transactional
    public void removeSupplier(Long skuId, Long supplierId) {
        skuLookupPort.requireExists(skuId);
        if (skuSupplierRepository.deleteBySkuIdAndSupplierId(skuId, supplierId) == 0) {
            throw new BusinessException(SkuSupplierErrorCode.SKU_SUPPLIER_NOT_FOUND);
        }
    }

    private Supplier requireSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));
    }

    private static SkuSupplierView toView(SkuSupplier mapping, Supplier supplier) {
        return new SkuSupplierView(supplier.getSupplierId(), supplier.getSupplierCode(), supplier.getName(),
                supplier.isActive(), mapping.getCreatedAt());
    }
}
