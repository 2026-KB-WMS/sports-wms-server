package com.kb.wms.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.application.port.in.AuthUseCase;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.ChangePasswordCommand;
import com.kb.wms.auth.application.port.in.command.LoginCommand;
import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.result.LoginResult;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.auth.exception.AuthErrorCode;
import com.kb.wms.common.config.SecurityConfig;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.JwtProvider;
import com.kb.wms.common.security.RestSecurityExceptionHandler;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class AuthControllerTest {

    private static final String SIGNUP_BODY = """
            {
              "loginId": "store_owner01",
              "password": "P@ssw0rd!",
              "name": "김점주",
              "email": "owner01@example.com",
              "phone": "010-1234-5678",
              "role": "STORE_OWNER"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private UserUseCase userUseCase;

    // SecurityConfig가 필터를 만들 때 필요하다. 요청에는 Authorization 헤더를 싣지 않아 파싱은 일어나지 않는다.
    @MockitoBean
    private JwtProvider jwtProvider;

    private User user(UserStatus status) {
        return User.builder()
                .userId(12L)
                .loginId("store_owner01")
                .passwordHash("hashed-secret")
                .name("김점주")
                .email("owner01@example.com")
                .phone("010-1234-5678")
                .role(UserRole.STORE_OWNER)
                .status(status)
                .lastLoginAt(LocalDateTime.of(2026, 9, 21, 2, 50))
                .createdAt(LocalDateTime.of(2026, 9, 20, 5, 0))
                .build();
    }

    private UsernamePasswordAuthenticationToken principal(UserRole role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(12L, role, List.of(), List.of()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    @DisplayName("가입에 성공하면 201과 PENDING 계정을 반환하고 비밀번호는 응답에 없다")
    void signUp_success() throws Exception {
        when(userUseCase.signUp(any(UserSignupCommand.class))).thenReturn(user(UserStatus.PENDING));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.userId").value(12))
                .andExpect(jsonPath("$.data.loginId").value("store_owner01"))
                .andExpect(jsonPath("$.data.role").value("STORE_OWNER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<UserSignupCommand> captor = ArgumentCaptor.forClass(UserSignupCommand.class);
        verify(userUseCase).signUp(captor.capture());
        assertThat(captor.getValue().loginId()).isEqualTo("store_owner01");
        assertThat(captor.getValue().role()).isEqualTo(UserRole.STORE_OWNER);
    }

    @Test
    @DisplayName("토큰 없이도 가입할 수 있다")
    void signUp_withoutToken() throws Exception {
        when(userUseCase.signUp(any(UserSignupCommand.class))).thenReturn(user(UserStatus.PENDING));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("필수 필드가 없거나 이메일 형식이 틀리면 400 VALIDATION_ERROR와 필드별 오류를 반환한다")
    void signUp_validationError() throws Exception {
        String body = """
                {
                  "loginId": "store_owner01",
                  "password": "P@ssw0rd!",
                  "email": "not-an-email",
                  "phone": "010-1234-5678",
                  "role": "STORE_OWNER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("role이 없으면 400, 알 수 없는 role 값도 400이다")
    void signUp_invalidRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY.replace("\"role\": \"STORE_OWNER\"", "\"role\": \"SUPER_USER\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY.replace(",\n  \"role\": \"STORE_OWNER\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'role')]").exists());

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("로그인 아이디가 중복이면 409 DUPLICATE_LOGIN_ID를 반환한다")
    void signUp_duplicateLoginId() throws Exception {
        when(userUseCase.signUp(any(UserSignupCommand.class)))
                .thenThrow(new BusinessException(AuthErrorCode.DUPLICATE_LOGIN_ID));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_LOGIN_ID"));
    }

    @Test
    @DisplayName("로그인에 성공하면 토큰과 사용자 요약을 반환한다")
    void login_success() throws Exception {
        when(authUseCase.login(any(LoginCommand.class)))
                .thenReturn(new LoginResult("access-token", 3600, user(UserStatus.ACTIVE)));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId": "store_owner01", "password": "P@ssw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andExpect(jsonPath("$.data.user.userId").value(12))
                .andExpect(jsonPath("$.data.user.role").value("STORE_OWNER"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.user.email").doesNotExist());

        ArgumentCaptor<LoginCommand> captor = ArgumentCaptor.forClass(LoginCommand.class);
        verify(authUseCase).login(captor.capture());
        assertThat(captor.getValue().loginId()).isEqualTo("store_owner01");
    }

    @Test
    @DisplayName("로그인 아이디나 비밀번호가 비어 있으면 400을 반환한다")
    void login_blank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId": "", "password": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'loginId')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());

        verifyNoInteractions(authUseCase);
    }

    @Test
    @DisplayName("아이디·비밀번호가 틀리면 401 UNAUTHORIZED를 반환한다")
    void login_unauthorized() throws Exception {
        when(authUseCase.login(any(LoginCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId": "store_owner01", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("승인 대기·비활성 계정은 403 ACCOUNT_PENDING·ACCOUNT_INACTIVE를 반환한다")
    void login_forbiddenAccount() throws Exception {
        when(authUseCase.login(any(LoginCommand.class)))
                .thenThrow(new BusinessException(AuthErrorCode.ACCOUNT_PENDING))
                .thenThrow(new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE));
        String body = """
                {"loginId": "store_owner01", "password": "P@ssw0rd!"}
                """;

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"));
    }

    @Test
    @DisplayName("내 정보 조회는 토큰 주체 본인의 계정을 반환한다")
    void getMe_success() throws Exception {
        when(userUseCase.getUser(12L)).thenReturn(user(UserStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/auth/me").with(authentication(principal(UserRole.STORE_OWNER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(12))
                .andExpect(jsonPath("$.data.loginId").value("store_owner01"))
                .andExpect(jsonPath("$.data.email").value("owner01@example.com"))
                .andExpect(jsonPath("$.data.lastLoginAt").exists())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(userUseCase).getUser(12L);
    }

    @Test
    @DisplayName("내 정보 조회에 토큰이 없으면 401 UNAUTHORIZED를 반환한다")
    void getMe_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("비밀번호 변경은 토큰 주체 본인에게 적용하고 응답에 비밀번호를 싣지 않는다")
    void changePassword_success() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .with(authentication(principal(UserRole.STORE_OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "Old@pass1", "newPassword": "New@pass1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<ChangePasswordCommand> captor = ArgumentCaptor.forClass(ChangePasswordCommand.class);
        verify(userUseCase).changePassword(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(12L);
        assertThat(captor.getValue().currentPassword()).isEqualTo("Old@pass1");
        assertThat(captor.getValue().newPassword()).isEqualTo("New@pass1");
    }

    @Test
    @DisplayName("비밀번호 변경에 토큰이 없으면 401, 필드가 비면 400이다")
    void changePassword_unauthorizedAndInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "Old@pass1", "newPassword": "New@pass1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .with(authentication(principal(UserRole.STORE_OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "", "newPassword": "New@pass1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("현재 비밀번호가 다르면 400 CURRENT_PASSWORD_MISMATCH를 반환한다")
    void changePassword_currentMismatch() throws Exception {
        doThrow(new BusinessException(AuthErrorCode.CURRENT_PASSWORD_MISMATCH))
                .when(userUseCase).changePassword(any(ChangePasswordCommand.class));

        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "Wrong@pass1", "newPassword": "New@pass1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    @DisplayName("토큰의 사용자가 삭제됐으면 404 USER_NOT_FOUND를 반환한다")
    void getMe_userMissing() throws Exception {
        when(userUseCase.getUser(12L)).thenThrow(new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/auth/me").with(authentication(principal(UserRole.HQ_ADMIN))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }
}
