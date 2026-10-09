package com.kb.wms.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
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

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RestSecurityExceptionHandler.class})
class UserControllerTest {

    private static final Long ADMIN_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

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
                .updatedAt(LocalDateTime.of(2026, 9, 21, 3, 10))
                .build();
    }

    private UsernamePasswordAuthenticationToken principal(UserRole role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(ADMIN_ID, role, List.of(), List.of()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    @Test
    @DisplayName("사용자 목록은 data.items로 반환하고 필터를 조건으로 넘긴다")
    void getUsers_success() throws Exception {
        when(userUseCase.getUsers(any(UserSearchCondition.class))).thenReturn(List.of(user(UserStatus.PENDING)));

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "STORE_OWNER")
                        .param("status", "PENDING")
                        .param("keyword", "김")
                        .with(authentication(principal(UserRole.HQ_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].userId").value(12))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].lastLoginAt").exists())
                .andExpect(jsonPath("$.data.items[0].createdAt").exists())
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());

        ArgumentCaptor<UserSearchCondition> captor = ArgumentCaptor.forClass(UserSearchCondition.class);
        verify(userUseCase).getUsers(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new UserSearchCondition(UserRole.STORE_OWNER, UserStatus.PENDING, "김"));
    }

    @Test
    @DisplayName("필터가 없으면 모든 조건을 null로 넘기고 결과가 없으면 빈 배열이다")
    void getUsers_noFilter() throws Exception {
        when(userUseCase.getUsers(any(UserSearchCondition.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users").with(authentication(principal(UserRole.HQ_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        verify(userUseCase).getUsers(new UserSearchCondition(null, null, null));
    }

    @Test
    @DisplayName("role·status 파라미터가 허용 목록 밖이면 400 VALIDATION_ERROR를 반환한다")
    void getUsers_invalidFilter() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("role", "SUPER_USER")
                        .with(authentication(principal(UserRole.HQ_ADMIN))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("사용자 목록은 토큰이 없으면 401, HQ_ADMIN이 아니면 403이다")
    void getUsers_authorization() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/users").with(authentication(principal(UserRole.WAREHOUSE_MANAGER))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/users").with(authentication(principal(UserRole.STORE_OWNER))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("사용자 수정은 요청한 관리자 ID와 함께 커맨드를 넘기고 수정 결과를 반환한다")
    void updateUser_success() throws Exception {
        when(userUseCase.updateUser(any(UserUpdateCommand.class))).thenReturn(user(UserStatus.ACTIVE));

        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "김점주", "phone": "010-9876-5432", "status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(12))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<UserUpdateCommand> captor = ArgumentCaptor.forClass(UserUpdateCommand.class);
        verify(userUseCase).updateUser(captor.capture());
        UserUpdateCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(12L);
        assertThat(command.actorUserId()).isEqualTo(ADMIN_ID);
        assertThat(command.phone()).isEqualTo("010-9876-5432");
        assertThat(command.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(command.email()).isNull();
    }

    @Test
    @DisplayName("loginId·password가 요청에 있어도 무시하지 않고 서비스로 넘겨 거절하게 한다")
    void updateUser_passesImmutableFieldsToService() throws Exception {
        when(userUseCase.updateUser(any(UserUpdateCommand.class)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "로그인 아이디와 비밀번호는 수정할 수 없습니다."));

        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId": "other_id", "password": "P@ssw0rd!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        ArgumentCaptor<UserUpdateCommand> captor = ArgumentCaptor.forClass(UserUpdateCommand.class);
        verify(userUseCase).updateUser(captor.capture());
        assertThat(captor.getValue().loginId()).isEqualTo("other_id");
        assertThat(captor.getValue().password()).isEqualTo("P@ssw0rd!");
    }

    @Test
    @DisplayName("형식·길이를 어기거나 알 수 없는 status면 400 VALIDATION_ERROR를 반환한다")
    void updateUser_validationError() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "phone": "0123456789012345678901234567890"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists());

        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DELETED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("name·email·phone을 빈 문자열이나 공백으로 보내면 500이 아니라 400을 반환한다")
    void updateUser_blankValues() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "", "phone": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'phone')]").exists());

        verifyNoInteractions(userUseCase);
    }

    @Test
    @DisplayName("userId가 숫자가 아니면 400을 반환한다")
    void updateUser_invalidPathVariable() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{userId}", "abc")
                        .with(authentication(principal(UserRole.HQ_ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "김점주"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("없는 사용자는 404 USER_NOT_FOUND, 이메일 중복은 409, 허용되지 않는 상태 전이는 409를 반환한다")
    void updateUser_domainErrors() throws Exception {
        when(userUseCase.updateUser(any(UserUpdateCommand.class)))
                .thenThrow(new BusinessException(AuthErrorCode.USER_NOT_FOUND))
                .thenThrow(new BusinessException(AuthErrorCode.DUPLICATE_EMAIL))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_USER_STATUS_TRANSITION))
                .thenThrow(new BusinessException(AuthErrorCode.AFFILIATION_REQUIRED));

        String[] expected = {"USER_NOT_FOUND", "DUPLICATE_EMAIL", "INVALID_USER_STATUS_TRANSITION", "AFFILIATION_REQUIRED"};
        int[] statuses = {404, 409, 409, 409};
        for (int i = 0; i < expected.length; i++) {
            mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                            .with(authentication(principal(UserRole.HQ_ADMIN)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status": "ACTIVE"}
                                    """))
                    .andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.errorCode").value(expected[i]));
        }
    }

    @Test
    @DisplayName("사용자 수정은 토큰이 없으면 401, HQ_ADMIN이 아니면 403이다")
    void updateUser_authorization() throws Exception {
        String body = """
                {"status": "ACTIVE"}
                """;

        mockMvc.perform(patch("/api/v1/users/{userId}", 12L).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/users/{userId}", 12L)
                        .with(authentication(principal(UserRole.STORE_OWNER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        verifyNoInteractions(userUseCase);
    }
}
