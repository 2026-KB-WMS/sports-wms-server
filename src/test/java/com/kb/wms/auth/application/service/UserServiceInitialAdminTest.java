package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kb.wms.auth.application.port.in.command.InitialHqAdminCommand;
import com.kb.wms.auth.application.port.in.result.InitialHqAdminResult;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.statushistory.application.port.in.StatusHistoryUseCase;

@ExtendWith(MockitoExtension.class)
class UserServiceInitialAdminTest {

    private static final String VALID_PASSWORD = "P@ssw0rd!";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserQueryRepository userQueryRepository;
    @Mock
    private StatusHistoryUseCase statusHistoryUseCase;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private InitialHqAdminCommand command(String loginId, String password, String name, String email, String phone) {
        return new InitialHqAdminCommand(loginId, password, name, email, phone);
    }

    private InitialHqAdminCommand validCommand() {
        return command("Hq_Admin01", VALID_PASSWORD, "홍길동", "admin@example.com", "010-0000-0000");
    }

    private void assertErrorCode(Runnable call, String expectedErrorCodeName) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(expectedErrorCodeName);
    }

    @Test
    @DisplayName("본사 관리자가 없으면 설정값으로 ACTIVE 관리자를 만들고 비밀번호는 해시하며 아이디는 소문자로 통일한다")
    void createsWhenNoHqAdmin() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);
        when(userRepository.existsByLoginId("hq_admin01")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("hashed");

        InitialHqAdminResult result = userService.ensureInitialHqAdmin(validCommand());

        assertThat(result).isEqualTo(InitialHqAdminResult.CREATED);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getLoginId()).isEqualTo("hq_admin01");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.HQ_ADMIN);
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("본사 관리자가 이미 있으면 설정이 있어도 아무것도 하지 않는다")
    void doesNothingWhenHqAdminExists() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(true);

        assertThat(userService.ensureInitialHqAdmin(validCommand())).isEqualTo(InitialHqAdminResult.ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자가 있으면 설정이 잘못돼 있어도 오류 없이 넘어간다")
    void existingAdminSkipsValidation() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(true);

        assertThat(userService.ensureInitialHqAdmin(command("x", "weak", "", "", "")))
                .isEqualTo(InitialHqAdminResult.ALREADY_EXISTS);
    }

    @Test
    @DisplayName("관리자가 없고 설정도 모두 비어 있으면 만들지 않고 NOT_CONFIGURED를 돌려준다")
    void notConfigured() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);

        assertThat(userService.ensureInitialHqAdmin(command("", null, " ", "", null)))
                .isEqualTo(InitialHqAdminResult.NOT_CONFIGURED);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("설정이 일부만 비어 있으면 어떤 값이 비었는지 알려 주며 400이다")
    void partiallyConfigured() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);

        assertThatThrownBy(() -> userService.ensureInitialHqAdmin(
                command("hq_admin01", VALID_PASSWORD, "홍길동", "", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email")
                .hasMessageContaining("phone")
                .hasMessageNotContaining("loginId")
                .hasMessageNotContaining(VALID_PASSWORD);
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "a234567890123456789012", "hq-admin", "관리자01"})
    @DisplayName("로그인 아이디가 가입과 같은 형식 규칙을 지키지 않으면 400이다")
    void invalidLoginId(String loginId) {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);

        assertErrorCode(() -> userService.ensureInitialHqAdmin(
                        command(loginId, VALID_PASSWORD, "홍길동", "admin@example.com", "010-0000-0000")),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1!", "onlyletters!!", "12345678!!", "NoSpecial123"})
    @DisplayName("비밀번호가 가입과 같은 규칙(8자 이상, 영문·숫자·특수문자)을 지키지 않으면 400이다")
    void invalidPassword(String password) {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);

        assertErrorCode(() -> userService.ensureInitialHqAdmin(
                        command("hq_admin01", password, "홍길동", "admin@example.com", "010-0000-0000")),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이름·이메일·연락처가 컬럼 길이를 넘거나 이메일 형식이 아니면 400이다")
    void invalidProfile() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);

        assertErrorCode(() -> userService.ensureInitialHqAdmin(
                        command("hq_admin01", VALID_PASSWORD, "가".repeat(101), "admin@example.com", "010")),
                ErrorCode.VALIDATION_ERROR.name());
        assertErrorCode(() -> userService.ensureInitialHqAdmin(
                        command("hq_admin01", VALID_PASSWORD, "홍길동", "not-an-email", "010")),
                ErrorCode.VALIDATION_ERROR.name());
        assertErrorCode(() -> userService.ensureInitialHqAdmin(
                        command("hq_admin01", VALID_PASSWORD, "홍길동", "admin@example.com", "0".repeat(31))),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 계정이 쓰는 아이디·이메일이면 409다")
    void duplicates() {
        when(userRepository.existsByRole(UserRole.HQ_ADMIN)).thenReturn(false);
        when(userRepository.existsByLoginId("hq_admin01")).thenReturn(true);

        assertErrorCode(() -> userService.ensureInitialHqAdmin(validCommand()), "DUPLICATE_LOGIN_ID");

        when(userRepository.existsByLoginId("hq_admin01")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        assertErrorCode(() -> userService.ensureInitialHqAdmin(validCommand()), "DUPLICATE_EMAIL");
        verify(userRepository, never()).save(any());
    }
}
