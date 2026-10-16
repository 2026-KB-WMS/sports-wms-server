package com.kb.wms.auth.adapter.in.web;

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
import com.kb.wms.auth.application.port.in.UserManagementUseCase;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.response.PageResponse;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 계정 관리(본사 관리자 전용, SecurityConfig가 HQ_ADMIN만 허용).
 * GET /api/v1/users, PATCH /api/v1/users/{userId}
 */
@Tag(name = "계정 관리")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final UserManagementUseCase userManagementUseCase;

    /** 목록 정렬 규칙. 기본은 가입 일시 최신순. */
    static final SortSpec LIST_SORT = SortSpec.builder()
            .field("createdAt", "u.createdAt")
            .field("name", "u.name")
            .field("loginId", "u.loginId")
            .field("role", "u.role")
            .field("status", "u.status")
            .defaultOrder("createdAt", SortDirection.DESC)
            .tieBreaker("u.userId")
            .build();

    @GetMapping
    public ApiResponse<PageResponse<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        PageCondition pageCondition = PageCondition.of(page, size, sort, LIST_SORT);
        return ApiResponse.ok(PageResponse.of(
                userUseCase.getUsers(new UserSearchCondition(role, status, keyword), pageCondition),
                UserSummaryResponse::of));
    }

    @PatchMapping("/{userId}")
    public ApiResponse<UserUpdateResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(UserUpdateResponse.of(
                userManagementUseCase.updateUser(request.toCommand(userId, principal.userId()))));
    }
}
