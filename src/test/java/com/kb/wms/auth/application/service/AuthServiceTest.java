package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kb.wms.auth.application.port.in.command.LoginCommand;
import com.kb.wms.auth.application.port.in.result.LoginResult;
import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.auth.exception.AuthErrorCode;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.JwtProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USER_ID = 12L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserQueryRepository userQueryRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider = new JwtProvider("test-only-jwt-secret-key-0123456789-0123456789", 3600);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userQueryRepository, passwordEncoder, jwtProvider);
    }

    private User userWith(UserStatus status) {
        return User.builder().userId(USER_ID).loginId("store_owner01").passwordHash("hashed")
                .name("김점주").email("owner01@example.com").phone("010-1234-5678")
                .role(UserRole.STORE_OWNER).status(status).build();
    }

    private BusinessException loginFailure(LoginCommand command) {
        BusinessException thrown = catchThrowableOfType(BusinessException.class, () -> authService.login(command));
        assertThat(thrown).isNotNull();
        return thrown;
    }

    @Test
    @DisplayName("ACTIVE 계정이 올바른 비밀번호로 로그인하면 토큰을 발급하고 마지막 로그인 시각을 기록한다")
    void login_success() {
        User user = userWith(UserStatus.ACTIVE);
        when(userRepository.findByLoginId("store_owner01")).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssw0rd!", "hashed")).thenReturn(true);
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of(3L)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = authService.login(new LoginCommand("store_owner01", "P@ssw0rd!"));

        assertThat(result.expiresIn()).isEqualTo(3600);
        assertThat(result.user().getLastLoginAt()).isNotNull();
        assertThat(jwtProvider.parse(result.accessToken()))
                .contains(new AuthenticatedUser(USER_ID, UserRole.STORE_OWNER, List.of(), List.of(3L)));
    }

    @Test
    @DisplayName("로그인 아이디는 공백을 없애고 소문자로 맞춰 조회한다")
    void login_normalizesLoginId() {
        User user = userWith(UserStatus.ACTIVE);
        when(userRepository.findByLoginId("store_owner01")).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("P@ssw0rd!", "hashed")).thenReturn(true);
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginResult result = authService.login(new LoginCommand(" Store_Owner01 ", "P@ssw0rd!"));

        assertThat(result.user().getLoginId()).isEqualTo("store_owner01");
    }

    @Test
    @DisplayName("없는 아이디와 비밀번호 불일치는 같은 401 메시지로 응답한다")
    void login_invalidCredentials() {
        when(userRepository.findByLoginId("nobody01")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("store_owner01")).thenReturn(Optional.of(userWith(UserStatus.ACTIVE)));
        when(passwordEncoder.matches("wrong-pass1!", "hashed")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");
        when(passwordEncoder.matches("P@ssw0rd!", "dummy-hash")).thenReturn(false);

        BusinessException unknownId = loginFailure(new LoginCommand("nobody01", "P@ssw0rd!"));
        BusinessException wrongPassword = loginFailure(new LoginCommand("store_owner01", "wrong-pass1!"));

        assertThat(unknownId.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(wrongPassword.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(unknownId.getMessage()).isEqualTo(wrongPassword.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("없는 아이디도 더미 해시와 비교해서 응답 시간 차이로 계정 존재 여부가 드러나지 않게 한다")
    void login_unknownIdComparesDummyHash() {
        when(userRepository.findByLoginId("nobody01")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("dummy-hash");

        loginFailure(new LoginCommand("nobody01", "P@ssw0rd!"));

        verify(passwordEncoder).matches("P@ssw0rd!", "dummy-hash");
    }

    @Test
    @DisplayName("비밀번호 확인 중 관리자가 계정을 비활성화했으면 잠금 후 다시 읽은 최신 상태로 거절하고 덮어쓰지 않는다")
    void login_statusChangedWhileVerifying() {
        when(userRepository.findByLoginId("store_owner01")).thenReturn(Optional.of(userWith(UserStatus.ACTIVE)));
        when(passwordEncoder.matches("P@ssw0rd!", "hashed")).thenReturn(true);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(userWith(UserStatus.INACTIVE)));

        assertThat(loginFailure(new LoginCommand("store_owner01", "P@ssw0rd!")).getErrorCodeName())
                .isEqualTo(AuthErrorCode.ACCOUNT_INACTIVE.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호가 맞아도 PENDING이면 ACCOUNT_PENDING, INACTIVE면 ACCOUNT_INACTIVE이고 토큰은 없다")
    void login_notActive() {
        when(userRepository.findByLoginId("store_owner01"))
                .thenReturn(Optional.of(userWith(UserStatus.PENDING)), Optional.of(userWith(UserStatus.INACTIVE)));
        when(userRepository.findByIdForUpdate(USER_ID))
                .thenReturn(Optional.of(userWith(UserStatus.PENDING)), Optional.of(userWith(UserStatus.INACTIVE)));
        when(passwordEncoder.matches("P@ssw0rd!", "hashed")).thenReturn(true);
        LoginCommand command = new LoginCommand("store_owner01", "P@ssw0rd!");

        assertThat(loginFailure(command).getErrorCodeName()).isEqualTo(AuthErrorCode.ACCOUNT_PENDING.name());
        assertThat(loginFailure(command).getErrorCodeName()).isEqualTo(AuthErrorCode.ACCOUNT_INACTIVE.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 PENDING 계정이어도 상태를 알려 주지 않고 401이다")
    void login_wrongPasswordHidesStatus() {
        when(userRepository.findByLoginId("store_owner01")).thenReturn(Optional.of(userWith(UserStatus.PENDING)));
        when(passwordEncoder.matches("wrong-pass1!", "hashed")).thenReturn(false);

        assertThat(loginFailure(new LoginCommand("store_owner01", "wrong-pass1!")).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("아이디나 비밀번호가 비어 있으면 VALIDATION_ERROR")
    void login_blank() {
        assertThat(loginFailure(new LoginCommand(" ", "P@ssw0rd!")).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(loginFailure(new LoginCommand("store_owner01", null)).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
