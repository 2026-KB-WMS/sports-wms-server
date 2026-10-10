package com.kb.wms.store.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.store.adapter.in.web.dto.request.StoreDeactivateRequest;
import com.kb.wms.store.adapter.in.web.dto.request.StoreRegisterRequest;
import com.kb.wms.store.adapter.in.web.dto.request.StoreUpdateRequest;
import com.kb.wms.store.adapter.in.web.dto.response.StoreMembershipResponse;
import com.kb.wms.store.adapter.in.web.dto.response.StoreResponse;
import com.kb.wms.store.adapter.in.web.dto.response.StoreSummaryResponse;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.domain.entity.Store;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 지점 등록/조회/수정/비활성화.
 * POST, GET, PATCH /api/v1/stores, GET /api/v1/stores/my
 * my 조회와 비활성화 처리자는 토큰 주체이고, 단건 조회는 서비스가 담당 지점(HQ_ADMIN은 전체)만 허용한다.
 */
@Tag(name = "지점")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreUseCase storeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> registerStore(@Valid @RequestBody StoreRegisterRequest request) {
        Store store = storeUseCase.registerStore(request.toCommand());
        return ApiResponse.created(StoreResponse.from(store));
    }

    @GetMapping
    public ApiResponse<ItemsResponse<StoreSummaryResponse>> getStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        List<StoreSummaryResponse> items = storeUseCase
                .getStores(new StoreSearchCondition(keyword, isActive)).stream()
                .map(StoreSummaryResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/my")
    public ApiResponse<ItemsResponse<StoreMembershipResponse>> getMyStores(
            @AuthenticationPrincipal AuthenticatedUser principal) {
        List<StoreMembershipResponse> items = storeUseCase.getMyStores(principal.userId()).stream()
                .map(StoreMembershipResponse::from)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<StoreResponse> getStore(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @PathVariable Long storeId) {
        Store store = storeUseCase.getStore(storeId, principal);
        return ApiResponse.ok(StoreResponse.from(store));
    }

    @PatchMapping("/{storeId}")
    public ApiResponse<StoreResponse> updateStore(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreUpdateRequest request) {
        Store store = storeUseCase.updateStore(storeId, request.toCommand());
        return ApiResponse.ok(StoreResponse.from(store));
    }

    @PatchMapping("/{storeId}/deactivate")
    public ApiResponse<StoreResponse> deactivateStore(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long storeId,
            @Valid @RequestBody(required = false) StoreDeactivateRequest request) {
        String reason = request == null ? null : request.reason();
        Store store = storeUseCase.deactivateStore(storeId, reason, principal.userId());
        return ApiResponse.ok(StoreResponse.from(store));
    }

    @PatchMapping("/{storeId}/activate")
    public ApiResponse<StoreResponse> activateStore(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long storeId) {
        Store store = storeUseCase.activateStore(storeId, principal.userId());
        return ApiResponse.ok(StoreResponse.from(store));
    }
}
