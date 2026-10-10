package com.kb.wms.auth.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kb.wms.auth.adapter.in.web.dto.request.ChangePasswordRequest;
import com.kb.wms.auth.adapter.in.web.dto.request.LoginRequest;
import com.kb.wms.auth.adapter.in.web.dto.request.SignupRequest;
import com.kb.wms.auth.adapter.in.web.dto.response.LoginResponse;
import com.kb.wms.auth.adapter.in.web.dto.response.MyProfileResponse;
import com.kb.wms.auth.adapter.in.web.dto.response.SignupResponse;
import com.kb.wms.auth.application.port.in.AuthUseCase;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.common.response.ApiResponse;
import com.kb.wms.common.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 가입·로그인·내 정보·비밀번호 변경.
 * POST /api/v1/auth/signup, POST /api/v1/auth/login, GET /api/v1/auth/me, PATCH /api/v1/auth/me/password
 */
@Tag(name = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserUseCase userUseCase;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signUp(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.created(SignupResponse.of(userUseCase.signUp(request.toCommand())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(LoginResponse.of(authUseCase.login(request.toCommand())));
    }

    /** 조회 대상은 토큰 주체 본인으로 고정한다. 인증 여부는 SecurityConfig가 검사한다. */
    @GetMapping("/me")
    public ApiResponse<MyProfileResponse> getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(MyProfileResponse.of(userUseCase.getUser(principal.userId())));
    }

    /** 변경 대상은 토큰 주체 본인으로 고정한다. */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userUseCase.changePassword(request.toCommand(principal.userId()));
        return ApiResponse.ok("비밀번호가 변경되었습니다.", null);
    }
}
