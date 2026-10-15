package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inbound.application.port.in.result.SkuSupplierView;
import com.kb.wms.inbound.application.port.out.SkuLookupPort;
import com.kb.wms.inbound.application.port.out.SkuSupplierRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.SkuSupplier;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SkuSupplierErrorCode;
import com.kb.wms.inbound.exception.SupplierErrorCode;
import com.kb.wms.product.exception.ProductErrorCode;

@ExtendWith(MockitoExtension.class)
class SkuSupplierServiceTest {

    @Mock
    private SkuSupplierRepository skuSupplierRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SkuLookupPort skuLookupPort;

    @InjectMocks
    private SkuSupplierService skuSupplierService;

    private static Supplier supplier(Long id, String code, String name, boolean active) {
        Supplier supplier = Supplier.builder()
                .supplierId(id).supplierCode(code).name(name).managerName("담당자")
                .contactNumber("02-555-1234").build();
        if (!active) {
            supplier.deactivate();
        }
        return supplier;
    }

    private static SkuSupplier mapping(Long skuId, Long supplierId) {
        return SkuSupplier.builder().skuSupplierId(1L).skuId(skuId).supplierId(supplierId)
                .createdAt(LocalDateTime.of(2026, 10, 15, 9, 0)).build();
    }

    private static void assertError(ThrowingCallable call, String errorCodeName) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(errorCodeName);
    }

    @Test
    @DisplayName("SKU에 매핑된 공급처를 매핑 순서대로 공급처 정보와 함께 돌려준다")
    void getSuppliers_success() {
        when(skuSupplierRepository.findBySkuId(10L)).thenReturn(List.of(mapping(10L, 3L), mapping(10L, 7L)));
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(supplier(3L, "SUP-A", "공급처 A", true)));
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier(7L, "SUP-B", "공급처 B", false)));

        List<SkuSupplierView> result = skuSupplierService.getSuppliers(10L);

        assertThat(result).extracting(SkuSupplierView::supplierId).containsExactly(3L, 7L);
        assertThat(result.get(0).supplierName()).isEqualTo("공급처 A");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).isActive()).isFalse();
        verify(skuLookupPort).requireExists(10L);
    }

    @Test
    @DisplayName("없는 SKU면 상품 도메인의 SKU_NOT_FOUND를 그대로 던지고 조회하지 않는다")
    void getSuppliers_skuNotFound() {
        doThrow(new BusinessException(ProductErrorCode.SKU_NOT_FOUND)).when(skuLookupPort).requireExists(999L);

        assertError(() -> skuSupplierService.getSuppliers(999L), ProductErrorCode.SKU_NOT_FOUND.name());
        verifyNoInteractions(skuSupplierRepository);
    }

    @Test
    @DisplayName("활성 공급처를 SKU에 매핑하면 저장하고 공급처 정보를 돌려준다")
    void register_success() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(supplier(3L, "SUP-A", "공급처 A", true)));
        when(skuSupplierRepository.existsBySkuIdAndSupplierId(10L, 3L)).thenReturn(false);
        when(skuSupplierRepository.save(any(SkuSupplier.class))).thenReturn(mapping(10L, 3L));

        SkuSupplierView result = skuSupplierService.registerSupplier(10L, 3L);

        assertThat(result.supplierId()).isEqualTo(3L);
        assertThat(result.supplierCode()).isEqualTo("SUP-A");
        assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 15, 9, 0));
    }

    @Test
    @DisplayName("없는 공급처는 SUPPLIER_NOT_FOUND, 비활성 공급처는 SUPPLIER_INACTIVE이고 저장하지 않는다")
    void register_supplierNotUsable() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());
        when(supplierRepository.findById(7L)).thenReturn(Optional.of(supplier(7L, "SUP-B", "공급처 B", false)));

        assertError(() -> skuSupplierService.registerSupplier(10L, 999L),
                SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
        assertError(() -> skuSupplierService.registerSupplier(10L, 7L),
                PurchaseOrderErrorCode.SUPPLIER_INACTIVE.name());
        verify(skuSupplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 매핑된 공급처는 SKU_SUPPLIER_ALREADY_MAPPED를 던진다")
    void register_alreadyMapped() {
        when(supplierRepository.findById(3L)).thenReturn(Optional.of(supplier(3L, "SUP-A", "공급처 A", true)));
        when(skuSupplierRepository.existsBySkuIdAndSupplierId(10L, 3L)).thenReturn(true);

        assertError(() -> skuSupplierService.registerSupplier(10L, 3L),
                SkuSupplierErrorCode.SKU_SUPPLIER_ALREADY_MAPPED.name());
        verify(skuSupplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("매핑을 삭제하고, 매핑이 없으면 SKU_SUPPLIER_NOT_FOUND를 던진다")
    void remove() {
        when(skuSupplierRepository.deleteBySkuIdAndSupplierId(10L, 3L)).thenReturn(1L);
        when(skuSupplierRepository.deleteBySkuIdAndSupplierId(10L, 4L)).thenReturn(0L);

        skuSupplierService.removeSupplier(10L, 3L);

        assertError(() -> skuSupplierService.removeSupplier(10L, 4L),
                SkuSupplierErrorCode.SKU_SUPPLIER_NOT_FOUND.name());
        verify(skuLookupPort, org.mockito.Mockito.times(2)).requireExists(10L);
    }
}
