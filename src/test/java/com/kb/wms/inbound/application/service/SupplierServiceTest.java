package com.kb.wms.inbound.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.application.port.out.PurchaseOrderRepository;
import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.exception.SupplierErrorCode;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @InjectMocks
    private SupplierService supplierService;

    private final SupplierRegisterCommand registerCommand = new SupplierRegisterCommand(
            "SUP-001", "공급처 A", "박담당", "02-555-1234", "sales@supplier-a.example.com", "경기도 성남시");

    private Supplier newSupplier() {
        return Supplier.register("SUP-001", "공급처 A", "박담당", "02-555-1234",
                "sales@supplier-a.example.com", "경기도 성남시");
    }

    @Test
    @DisplayName("공급처 코드가 중복되지 않으면 등록에 성공한다")
    void registerSupplier_success() {
        when(supplierRepository.existsBySupplierCode("SUP-001")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.registerSupplier(registerCommand);

        assertThat(result.getSupplierCode()).isEqualTo("SUP-001");
        assertThat(result.getName()).isEqualTo("공급처 A");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("공급처 코드가 중복되면 DUPLICATE_SUPPLIER_CODE 예외를 던진다")
    void registerSupplier_duplicateCode() {
        when(supplierRepository.existsBySupplierCode("SUP-001")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.registerSupplier(registerCommand))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(SupplierErrorCode.DUPLICATE_SUPPLIER_CODE.name());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("공급처 목록 조회는 검색 조건을 리포지토리에 전달해 결과를 그대로 반환한다")
    void getSuppliers_returnsResult() {
        SupplierSearchCondition condition = new SupplierSearchCondition("공급", true);
        when(supplierRepository.search(condition)).thenReturn(List.of(newSupplier()));

        List<Supplier> result = supplierService.getSuppliers(condition);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSupplierCode()).isEqualTo("SUP-001");
    }

    @Test
    @DisplayName("공급처를 조회하면 그대로 반환한다")
    void getSupplier_success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(newSupplier()));

        Supplier result = supplierService.getSupplier(1L);

        assertThat(result.getSupplierCode()).isEqualTo("SUP-001");
    }

    @Test
    @DisplayName("존재하지 않는 공급처를 조회하면 SUPPLIER_NOT_FOUND 예외를 던진다")
    void getSupplier_notFound() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplier(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("변경할 필드가 없으면 VALIDATION_ERROR 예외를 던진다")
    void updateSupplier_noChanges_throwsBusinessException() {
        SupplierUpdateCommand command = new SupplierUpdateCommand(null, null, null, null, null, false, false);

        assertThatThrownBy(() -> supplierService.updateSupplier(1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        verify(supplierRepository, never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 공급처를 수정하면 SUPPLIER_NOT_FOUND 예외를 던진다")
    void updateSupplier_notFound() {
        SupplierUpdateCommand command = new SupplierUpdateCommand("새 이름", null, null, null, null, false, false);
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.updateSupplier(999L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("일부 필드만 변경하면 해당 필드만 수정되어 저장된다")
    void updateSupplier_partialUpdate_success() {
        Supplier existing = newSupplier();
        SupplierUpdateCommand command = new SupplierUpdateCommand(null, "최담당", "02-555-9999", null, null, false, false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.updateSupplier(1L, command);

        assertThat(result.getManagerName()).isEqualTo("최담당");
        assertThat(result.getContactNumber()).isEqualTo("02-555-9999");
        assertThat(result.getName()).isEqualTo("공급처 A");
        assertThat(result.getEmail()).isEqualTo("sales@supplier-a.example.com");
        assertThat(result.getAddress()).isEqualTo("경기도 성남시");
        verify(supplierRepository).save(existing);
    }

    @Test
    @DisplayName("email·address를 비움으로 지정하면 값이 null로 지워진다")
    void updateSupplier_clearEmailAndAddress() {
        Supplier existing = newSupplier();
        SupplierUpdateCommand command = new SupplierUpdateCommand(null, null, null, null, null, true, true);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.updateSupplier(1L, command);

        assertThat(result.getEmail()).isNull();
        assertThat(result.getAddress()).isNull();
        assertThat(result.getName()).isEqualTo("공급처 A");
    }

    @Test
    @DisplayName("비움 플래그가 없으면 email·address는 그대로 유지된다")
    void updateSupplier_withoutClearFlag_keepsEmailAndAddress() {
        Supplier existing = newSupplier();
        SupplierUpdateCommand command = new SupplierUpdateCommand("새 이름", null, null, null, null, false, false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.updateSupplier(1L, command);

        assertThat(result.getEmail()).isEqualTo("sales@supplier-a.example.com");
        assertThat(result.getAddress()).isEqualTo("경기도 성남시");
    }

    @Test
    @DisplayName("값과 비움 플래그가 함께 오면 값이 우선한다")
    void updateSupplier_valueTakesPrecedenceOverClearFlag() {
        Supplier existing = newSupplier();
        SupplierUpdateCommand command = new SupplierUpdateCommand(null, null, null, "new@supplier-a.example.com", null, true, false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.updateSupplier(1L, command);

        assertThat(result.getEmail()).isEqualTo("new@supplier-a.example.com");
    }

    @Test
    @DisplayName("비활성 공급처도 정보를 수정할 수 있다")
    void updateSupplier_inactiveSupplier_success() {
        Supplier inactive = newSupplier();
        inactive.deactivate();
        SupplierUpdateCommand command = new SupplierUpdateCommand("새 이름", null, null, null, null, false, false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(inactive));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.updateSupplier(1L, command);

        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 공급처를 비활성화하면 SUPPLIER_NOT_FOUND 예외를 던진다")
    void deactivateSupplier_notFound() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.deactivateSupplier(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(SupplierErrorCode.SUPPLIER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("이미 비활성화된 공급처를 다시 비활성화하면 CONFLICT 예외를 던진다")
    void deactivateSupplier_alreadyInactive_throwsConflict() {
        Supplier inactive = newSupplier();
        inactive.deactivate();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> supplierService.deactivateSupplier(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(ErrorCode.CONFLICT.name());
        verify(supplierRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 공급처를 비활성화하면 성공한다")
    void deactivateSupplier_success() {
        Supplier active = newSupplier();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(active));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.deactivateSupplier(1L);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("진행 중인 발주가 있는 공급처를 비활성화하면 SUPPLIER_IN_USE 예외를 던진다")
    void deactivateSupplier_inProgressPurchaseOrder_throwsInUse() {
        Supplier active = newSupplier();
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(active));
        when(purchaseOrderRepository.existsInProgressBySupplierId(1L)).thenReturn(true);

        assertThatThrownBy(() -> supplierService.deactivateSupplier(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(SupplierErrorCode.SUPPLIER_IN_USE.name());
        assertThat(active.isActive()).isTrue();
        verify(supplierRepository, never()).save(any());
    }
}
