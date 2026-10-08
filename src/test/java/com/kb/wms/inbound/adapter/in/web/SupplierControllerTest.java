package com.kb.wms.inbound.adapter.in.web;

import static com.kb.wms.common.security.TestAuth.signInAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.inbound.application.port.in.SupplierUseCase;
import com.kb.wms.inbound.application.port.in.command.SupplierRegisterCommand;
import com.kb.wms.inbound.application.port.in.command.SupplierUpdateCommand;
import com.kb.wms.inbound.application.port.in.query.SupplierSearchCondition;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.inbound.exception.SupplierErrorCode;

@WebMvcTest(SupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
class SupplierControllerTest {

    private static final String REGISTER_BODY = """
            {
              "supplierCode": "SUP-001",
              "supplierName": "공급처 A",
              "managerName": "박담당",
              "contactNumber": "02-555-1234",
              "email": "sales@supplier-a.example.com",
              "address": "경기도 성남시"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void signIn() {
        // 처리 사용자는 토큰 주체다. 서비스는 목이라 담당 창고 범위는 서비스 테스트에서 확인한다.
        signInAs(UserRole.WAREHOUSE_MANAGER, 5L, java.util.List.of(1L), java.util.List.of());
    }

    @MockitoBean
    private SupplierUseCase supplierUseCase;

    private Supplier newSupplier() {
        return Supplier.register("SUP-001", "공급처 A", "박담당", "02-555-1234",
                "sales@supplier-a.example.com", "경기도 성남시");
    }

    private void performUpdate(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("공급처 등록에 성공하면 201과 등록된 공급처를 반환한다")
    void registerSupplier_success() throws Exception {
        when(supplierUseCase.registerSupplier(any(SupplierRegisterCommand.class))).thenReturn(newSupplier());

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-001"))
                .andExpect(jsonPath("$.data.supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("공급처 코드가 중복되면 409 DUPLICATE_SUPPLIER_CODE를 반환한다")
    void registerSupplier_duplicateCode() throws Exception {
        when(supplierUseCase.registerSupplier(any(SupplierRegisterCommand.class)))
                .thenThrow(new BusinessException(SupplierErrorCode.DUPLICATE_SUPPLIER_CODE));

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_SUPPLIER_CODE"));
    }

    @Test
    @DisplayName("필수 필드가 없으면 등록 요청은 400 VALIDATION_ERROR를 반환한다")
    void registerSupplier_missingRequiredField() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "supplierCode": "SUP-001", "supplierName": "공급처 A" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 등록 요청은 400 VALIDATION_ERROR를 반환한다")
    void registerSupplier_invalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY.replace("sales@supplier-a.example.com", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("공급처 목록을 조회하면 200과 items 배열을 반환한다")
    void getSuppliers_success() throws Exception {
        when(supplierUseCase.getSuppliers(eq(new SupplierSearchCondition("공급", true)), any()))
                .thenReturn(List.of(newSupplier()));

        mockMvc.perform(get("/api/v1/suppliers").param("keyword", "공급").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].supplierCode").value("SUP-001"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.items[0].isActive").value(true));
    }

    @Test
    @DisplayName("공급처 상세를 조회하면 200과 공급처를 반환한다")
    void getSupplier_success() throws Exception {
        when(supplierUseCase.getSupplier(eq(1L), any())).thenReturn(newSupplier());

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-001"))
                .andExpect(jsonPath("$.data.managerName").value("박담당"));
    }

    @Test
    @DisplayName("존재하지 않는 공급처를 조회하면 404 SUPPLIER_NOT_FOUND를 반환한다")
    void getSupplier_notFound() throws Exception {
        when(supplierUseCase.getSupplier(eq(999L), any()))
                .thenThrow(new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_NOT_FOUND"));
    }

    @Test
    @DisplayName("공급처를 수정하면 200과 수정된 공급처를 반환한다")
    void updateSupplier_success() throws Exception {
        Supplier updated = Supplier.register("SUP-001", "새 이름", "박담당", "02-555-1234", null, null);
        when(supplierUseCase.updateSupplier(eq(1L), any(SupplierUpdateCommand.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "supplierName": "새 이름" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierName").value("새 이름"));
    }

    @Test
    @DisplayName("보내지 않은 필드는 변경 없음으로 전달한다")
    void updateSupplier_absentKeys_areNotChanged() throws Exception {
        when(supplierUseCase.updateSupplier(eq(1L), any(SupplierUpdateCommand.class))).thenReturn(newSupplier());

        performUpdate("""
                { "managerName": "최담당" }
                """);

        ArgumentCaptor<SupplierUpdateCommand> captor = ArgumentCaptor.forClass(SupplierUpdateCommand.class);
        verify(supplierUseCase).updateSupplier(eq(1L), captor.capture());
        SupplierUpdateCommand command = captor.getValue();
        assertThat(command.managerName()).isEqualTo("최담당");
        assertThat(command.name()).isNull();
        assertThat(command.email()).isNull();
        assertThat(command.address()).isNull();
        assertThat(command.clearEmail()).isFalse();
        assertThat(command.clearAddress()).isFalse();
    }

    @Test
    @DisplayName("email·address에 null을 명시하면 비움으로 전달한다")
    void updateSupplier_explicitNull_clearsEmailAndAddress() throws Exception {
        when(supplierUseCase.updateSupplier(eq(1L), any(SupplierUpdateCommand.class))).thenReturn(newSupplier());

        performUpdate("""
                { "email": null, "address": null }
                """);

        ArgumentCaptor<SupplierUpdateCommand> captor = ArgumentCaptor.forClass(SupplierUpdateCommand.class);
        verify(supplierUseCase).updateSupplier(eq(1L), captor.capture());
        SupplierUpdateCommand command = captor.getValue();
        assertThat(command.email()).isNull();
        assertThat(command.address()).isNull();
        assertThat(command.clearEmail()).isTrue();
        assertThat(command.clearAddress()).isTrue();
        assertThat(command.hasNoChanges()).isFalse();
    }

    @Test
    @DisplayName("필수 항목(managerName)에 null을 명시하면 400 VALIDATION_ERROR를 반환한다")
    void updateSupplier_nullRequiredField_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "managerName": null }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("필수 항목(contactNumber)을 빈 문자열로 보내면 400 VALIDATION_ERROR를 반환한다")
    void updateSupplier_blankRequiredField_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "contactNumber": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 수정 요청은 400 VALIDATION_ERROR를 반환한다")
    void updateSupplier_invalidEmail_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "not-an-email" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("supplierCode·isActive 필드를 함께 보내 수정을 요청하면 400 VALIDATION_ERROR를 반환한다")
    void updateSupplier_immutableFields_returnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "supplierCode": "SUP-999" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "isActive": false }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supplierUseCase);
    }

    @Test
    @DisplayName("수정할 필드가 하나도 없으면 400 VALIDATION_ERROR를 반환한다")
    void updateSupplier_noFields_returnsValidationError() throws Exception {
        when(supplierUseCase.updateSupplier(eq(1L), any(SupplierUpdateCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요."));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("공급처를 비활성화하면 200과 명세의 비활성화 응답 필드를 반환한다")
    void deactivateSupplier_success() throws Exception {
        Supplier supplier = newSupplier();
        supplier.deactivate();
        when(supplierUseCase.deactivateSupplier(1L)).thenReturn(supplier);

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/deactivate", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierCode").value("SUP-001"))
                .andExpect(jsonPath("$.data.supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.managerName").doesNotExist());
    }

    @Test
    @DisplayName("이미 비활성화된 공급처를 비활성화하면 409 CONFLICT를 반환한다")
    void deactivateSupplier_alreadyInactive() throws Exception {
        when(supplierUseCase.deactivateSupplier(1L))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 비활성화된 공급처입니다."));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/deactivate", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("존재하지 않는 공급처를 비활성화하면 404 SUPPLIER_NOT_FOUND를 반환한다")
    void deactivateSupplier_notFound() throws Exception {
        when(supplierUseCase.deactivateSupplier(999L))
                .thenThrow(new BusinessException(SupplierErrorCode.SUPPLIER_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/deactivate", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_NOT_FOUND"));
    }
}
