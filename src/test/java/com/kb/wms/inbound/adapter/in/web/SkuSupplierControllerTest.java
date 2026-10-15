package com.kb.wms.inbound.adapter.in.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.inbound.application.port.in.SkuSupplierUseCase;
import com.kb.wms.inbound.application.port.in.result.SkuSupplierView;
import com.kb.wms.inbound.exception.PurchaseOrderErrorCode;
import com.kb.wms.inbound.exception.SkuSupplierErrorCode;
import com.kb.wms.product.exception.ProductErrorCode;

@WebMvcTest(SkuSupplierController.class)
@AutoConfigureMockMvc(addFilters = false)
class SkuSupplierControllerTest {

    private static final SkuSupplierView VIEW =
            new SkuSupplierView(3L, "SUP-A", "공급처 A", true, LocalDateTime.of(2026, 10, 15, 9, 0));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkuSupplierUseCase skuSupplierUseCase;

    @Test
    @DisplayName("SKU에 매핑된 공급처를 data.items로 반환한다")
    void getSuppliers_success() throws Exception {
        when(skuSupplierUseCase.getSuppliers(10L)).thenReturn(List.of(VIEW));

        mockMvc.perform(get("/api/v1/products/skus/{skuId}/suppliers", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].supplierId").value(3))
                .andExpect(jsonPath("$.data.items[0].supplierCode").value("SUP-A"))
                .andExpect(jsonPath("$.data.items[0].supplierName").value("공급처 A"))
                .andExpect(jsonPath("$.data.items[0].isActive").value(true))
                .andExpect(jsonPath("$.data.items[0].createdAt").exists());
    }

    @Test
    @DisplayName("없는 SKU면 404 SKU_NOT_FOUND를 반환한다")
    void getSuppliers_skuNotFound() throws Exception {
        when(skuSupplierUseCase.getSuppliers(999L))
                .thenThrow(new BusinessException(ProductErrorCode.SKU_NOT_FOUND));

        mockMvc.perform(get("/api/v1/products/skus/{skuId}/suppliers", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SKU_NOT_FOUND"));
    }

    @Test
    @DisplayName("매핑을 등록하면 201과 공급처 정보를 반환한다")
    void register_success() throws Exception {
        when(skuSupplierUseCase.registerSupplier(10L, 3L)).thenReturn(VIEW);

        mockMvc.perform(post("/api/v1/products/skus/{skuId}/suppliers", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"supplierId\": 3 }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.supplierId").value(3))
                .andExpect(jsonPath("$.data.supplierName").value("공급처 A"));
    }

    @Test
    @DisplayName("공급처 ID가 없으면 400 VALIDATION_ERROR이고 서비스를 호출하지 않는다")
    void register_validation() throws Exception {
        mockMvc.perform(post("/api/v1/products/skus/{skuId}/suppliers", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(skuSupplierUseCase);
    }

    @Test
    @DisplayName("비활성 공급처는 409 SUPPLIER_INACTIVE, 이미 매핑된 공급처는 409 SKU_SUPPLIER_ALREADY_MAPPED다")
    void register_conflicts() throws Exception {
        when(skuSupplierUseCase.registerSupplier(10L, 7L))
                .thenThrow(new BusinessException(PurchaseOrderErrorCode.SUPPLIER_INACTIVE));
        when(skuSupplierUseCase.registerSupplier(10L, 3L))
                .thenThrow(new BusinessException(SkuSupplierErrorCode.SKU_SUPPLIER_ALREADY_MAPPED));

        mockMvc.perform(post("/api/v1/products/skus/{skuId}/suppliers", 10L)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"supplierId\": 7 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SUPPLIER_INACTIVE"));
        mockMvc.perform(post("/api/v1/products/skus/{skuId}/suppliers", 10L)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"supplierId\": 3 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SKU_SUPPLIER_ALREADY_MAPPED"));
    }

    @Test
    @DisplayName("매핑을 삭제하면 200과 삭제한 SKU·공급처 ID를 반환한다")
    void remove_success() throws Exception {
        mockMvc.perform(delete("/api/v1/products/skus/{skuId}/suppliers/{supplierId}", 10L, 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuId").value(10))
                .andExpect(jsonPath("$.data.supplierId").value(3));
        verify(skuSupplierUseCase).removeSupplier(10L, 3L);
    }

    @Test
    @DisplayName("없는 매핑을 삭제하면 404 SKU_SUPPLIER_NOT_FOUND를 반환한다")
    void remove_notFound() throws Exception {
        doThrow(new BusinessException(SkuSupplierErrorCode.SKU_SUPPLIER_NOT_FOUND))
                .when(skuSupplierUseCase).removeSupplier(10L, 4L);

        mockMvc.perform(delete("/api/v1/products/skus/{skuId}/suppliers/{supplierId}", 10L, 4L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SKU_SUPPLIER_NOT_FOUND"));
    }
}
