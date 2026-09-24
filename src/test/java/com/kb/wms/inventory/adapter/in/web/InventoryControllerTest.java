package com.kb.wms.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.inventory.application.port.in.InventoryAdjustmentUseCase;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;

/**
 * GlobalExceptionHandler의 MethodArgumentTypeMismatchException 처리 검증(#55).
 * 숫자가 아닌 path variable을 넘기면 500이 아니라 400 VALIDATION_ERROR가 나와야 한다.
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryQueryUseCase inventoryQueryUseCase;
    @MockitoBean
    private InventoryAdjustmentUseCase inventoryAdjustmentUseCase;

    @Test
    @DisplayName("inventoryId가 숫자가 아니면 400 VALIDATION_ERROR를 반환한다")
    void getInventory_nonNumericId_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }
}
