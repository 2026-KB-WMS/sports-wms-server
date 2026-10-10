package com.kb.wms.store.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.store.adapter.in.web.dto.request.StoreMemberAssignRequest;
import com.kb.wms.store.adapter.in.web.dto.response.StoreMemberReleaseResponse;
import com.kb.wms.store.adapter.in.web.dto.response.StoreMemberResponse;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.result.StoreMemberView;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 지점 관리자(점주) 배정/조회/배정해제.
 * POST /api/v1/stores/assign, GET /api/v1/stores/managers, DELETE /api/v1/stores/managers/{storeMemberId}
 */
@Tag(name = "지점 담당자")
@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreMemberController {

    private final StoreMemberUseCase storeMemberUseCase;
    private final StoreUseCase storeUseCase;

    @PostMapping("/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreMemberResponse> assignManager(@Valid @RequestBody StoreMemberAssignRequest request) {
        StoreMemberView member = storeMemberUseCase.assignManager(request.toCommand());
        return ApiResponse.created(toResponse(member));
    }

    @GetMapping("/managers")
    public ApiResponse<ItemsResponse<StoreMemberResponse>> getManagers(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        List<StoreMemberResponse> items = storeMemberUseCase.getManagers(storeId, userId, keyword).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @DeleteMapping("/managers/{storeMemberId}")
    public ApiResponse<StoreMemberReleaseResponse> releaseManager(@PathVariable Long storeMemberId) {
        storeMemberUseCase.releaseManager(storeMemberId);
        return ApiResponse.ok(new StoreMemberReleaseResponse(storeMemberId));
    }

    private StoreMemberResponse toResponse(StoreMemberView member) {
        String storeName = storeUseCase.getStore(member.storeId()).getName();
        return StoreMemberResponse.of(member, storeName);
    }
}
