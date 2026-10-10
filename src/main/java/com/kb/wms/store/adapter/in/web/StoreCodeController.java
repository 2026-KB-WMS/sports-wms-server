package com.kb.wms.store.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.store.adapter.in.web.dto.response.CodeItemResponse;
import com.kb.wms.store.application.port.in.StoreCodeUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 지점 도메인 고정 코드 목록 조회.
 * GET /api/v1/stores/management-types
 */
@Tag(name = "지점 코드")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreCodeController {

    private final StoreCodeUseCase storeCodeUseCase;

    @GetMapping("/management-types")
    public ApiResponse<ItemsResponse<CodeItemResponse>> getManagementTypes() {
        List<CodeItemResponse> items = storeCodeUseCase.getManagementTypes().stream()
                .map(CodeItemResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }
}
