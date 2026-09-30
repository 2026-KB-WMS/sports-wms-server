package com.kb.wms.store.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.store.application.port.in.StoreCodeUseCase;
import com.kb.wms.store.application.port.in.result.CodeItem;

@WebMvcTest(StoreCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreCodeUseCase storeCodeUseCase;

    @Test
    @DisplayName("담당 역할 코드 목록을 조회하면 200과 items 배열로 감싼 응답을 반환한다")
    void getManagementTypes_success() throws Exception {
        when(storeCodeUseCase.getManagementTypes())
                .thenReturn(List.of(new CodeItem("OWNER", "점주"), new CodeItem("MANAGER", "지점 관리자")));

        mockMvc.perform(get("/api/v1/stores/management-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value("OWNER"))
                .andExpect(jsonPath("$.data.items[1].code").value("MANAGER"));
    }
}
