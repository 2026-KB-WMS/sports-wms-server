package com.kb.wms.auth.adapter.in.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.auth.adapter.in.web.dto.request.UserUpdateRequest;
import com.kb.wms.auth.adapter.in.web.dto.response.UserSummaryResponse;
import com.kb.wms.auth.adapter.in.web.dto.response.UserUpdateResponse;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.ItemsResponse;
import com.kb.wms.common.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 계정 관리(본사 관리자 전용, SecurityConfig가 HQ_ADMIN만 허용).
 * GET /api/v1/users, PATCH /api/v1/users/{userId}
 * 페이지네이션은 전 도메인 일괄 도입 시 적용한다.
 */
@Tag(name = "계정 관리")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;

    @GetMapping
    public ApiResponse<ItemsResponse<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword) {
        List<UserSummaryResponse> items = userUseCase.getUsers(new UserSearchCondition(role, status, keyword))
                .stream()
                .map(UserSummaryResponse::of)
                .toList();
        return ApiResponse.ok(ItemsResponse.of(items));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<UserUpdateResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(UserUpdateResponse.of(
                userUseCase.updateUser(request.toCommand(userId, principal.userId()))));
    }
}
