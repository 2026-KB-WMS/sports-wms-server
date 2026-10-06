package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.auth.exception.AuthErrorCode;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.statushistory.application.port.in.StatusHistoryUseCase;
import com.kb.wms.common.statushistory.domain.enums.StatusHistoryEntityType;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USER_ID = 12L;
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

    private UserSignupCommand signupCommand(String loginId, String password, UserRole role) {
        return new UserSignupCommand(loginId, password, "김점주", "owner01@example.com", "010-1234-5678", role);
    }

    private User userWith(Long userId, UserRole role, UserStatus status) {
        return User.builder().userId(userId).loginId("store_owner01").passwordHash("hashed")
                .name("김점주").email("owner01@example.com").phone("010-1234-5678")
                .role(role).status(status).build();
    }

    private UserUpdateCommand statusUpdate(Long userId, UserStatus status) {
        return new UserUpdateCommand(userId, ADMIN_ID, null, null, null, null, null, null, status);
    }

    private void assertErrorCode(Runnable call, String expectedErrorCodeName) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCodeName())
                .isEqualTo(expectedErrorCodeName);
    }

    @Test
    @DisplayName("가입하면 PENDING으로 저장하고 비밀번호는 해시한다")
    void signUp_success() {
        when(userRepository.existsByLoginId("store_owner01")).thenReturn(false);
        when(userRepository.existsByEmail("owner01@example.com")).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.signUp(signupCommand("store_owner01", VALID_PASSWORD, UserRole.STORE_OWNER));

        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
        assertThat(result.getRole()).isEqualTo(UserRole.STORE_OWNER);
    }

    @Test
    @DisplayName("로그인 아이디는 소문자로 통일해 저장한다")
    void signUp_lowercasesLoginId() {
        when(userRepository.existsByLoginId("store_owner01")).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.signUp(signupCommand("Store_Owner01", VALID_PASSWORD, UserRole.STORE_OWNER));

        assertThat(result.getLoginId()).isEqualTo("store_owner01");
    }

    @Test
    @DisplayName("HQ_ADMIN 가입은 거절한다")
    void signUp_hqAdminRejected() {
        assertErrorCode(
                () -> userService.signUp(signupCommand("admin01", VALID_PASSWORD, UserRole.HQ_ADMIN)),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("역할이 없으면 가입을 거절한다")
    void signUp_roleMissing() {
        assertErrorCode(
                () -> userService.signUp(signupCommand("store_owner01", VALID_PASSWORD, null)),
                ErrorCode.VALIDATION_ERROR.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "a234567890123456789012", "store-owner", "스토어01", "own er"})
    @DisplayName("로그인 아이디 형식을 지키지 않으면 가입을 거절한다")
    void signUp_invalidLoginId(String loginId) {
        assertErrorCode(
                () -> userService.signUp(signupCommand(loginId, VALID_PASSWORD, UserRole.STORE_OWNER)),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"P@ss1", "Password!!", "Password123", "12345678!", "abcdefgh!"})
    @DisplayName("비밀번호가 8자 미만이거나 영문·숫자·특수문자 중 하나라도 빠지면 가입을 거절한다")
    void signUp_invalidPassword(String password) {
        assertErrorCode(
                () -> userService.signUp(signupCommand("store_owner01", password, UserRole.STORE_OWNER)),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호가 72바이트를 넘으면 가입을 거절한다")
    void signUp_passwordTooLong() {
        String tooLong = "P@ss1" + "a".repeat(68);

        assertErrorCode(
                () -> userService.signUp(signupCommand("store_owner01", tooLong, UserRole.STORE_OWNER)),
                ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("이미 사용 중인 로그인 아이디면 DUPLICATE_LOGIN_ID")
    void signUp_duplicateLoginId() {
        when(userRepository.existsByLoginId("store_owner01")).thenReturn(true);

        assertErrorCode(
                () -> userService.signUp(signupCommand("store_owner01", VALID_PASSWORD, UserRole.STORE_OWNER)),
                AuthErrorCode.DUPLICATE_LOGIN_ID.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL")
    void signUp_duplicateEmail() {
        when(userRepository.existsByLoginId("store_owner01")).thenReturn(false);
        when(userRepository.existsByEmail("owner01@example.com")).thenReturn(true);

        assertErrorCode(
                () -> userService.signUp(signupCommand("store_owner01", VALID_PASSWORD, UserRole.STORE_OWNER)),
                AuthErrorCode.DUPLICATE_EMAIL.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("목록 조회는 조건을 그대로 저장소에 넘긴다")
    void getUsers() {
        UserSearchCondition condition = new UserSearchCondition(UserRole.STORE_OWNER, UserStatus.PENDING, "김");
        List<User> users = List.of(userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.PENDING));
        when(userRepository.search(condition)).thenReturn(users);

        assertThat(userService.getUsers(condition)).isEqualTo(users);
    }

    @Test
    @DisplayName("loginId나 password가 포함된 수정 요청은 거절한다")
    void updateUser_loginIdOrPasswordRejected() {
        UserUpdateCommand withLoginId =
                new UserUpdateCommand(USER_ID, ADMIN_ID, "new_id", null, "이름", null, null, null, null);
        UserUpdateCommand withPassword =
                new UserUpdateCommand(USER_ID, ADMIN_ID, null, VALID_PASSWORD, "이름", null, null, null, null);

        assertErrorCode(() -> userService.updateUser(withLoginId), ErrorCode.VALIDATION_ERROR.name());
        assertErrorCode(() -> userService.updateUser(withPassword), ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 필드가 하나도 없으면 거절한다")
    void updateUser_noChanges() {
        UserUpdateCommand command =
                new UserUpdateCommand(USER_ID, ADMIN_ID, null, null, null, null, null, null, null);

        assertErrorCode(() -> userService.updateUser(command), ErrorCode.VALIDATION_ERROR.name());
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND")
    void updateUser_notFound() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE)),
                AuthErrorCode.USER_NOT_FOUND.name());
    }

    @Test
    @DisplayName("보낸 필드만 수정하고 상태가 그대로면 이력을 남기지 않는다")
    void updateUser_partialUpdate() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndUserIdNot("new@example.com", USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(new UserUpdateCommand(
                USER_ID, ADMIN_ID, null, null, "박점주", "new@example.com", null, null, null));

        assertThat(result.getName()).isEqualTo("박점주");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPhone()).isEqualTo("010-1234-5678");
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 계정이 쓰는 이메일로 바꾸면 DUPLICATE_EMAIL")
    void updateUser_duplicateEmail() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndUserIdNot("taken@example.com", USER_ID)).thenReturn(true);

        assertErrorCode(() -> userService.updateUser(new UserUpdateCommand(
                        USER_ID, ADMIN_ID, null, null, null, "taken@example.com", null, null, null)),
                AuthErrorCode.DUPLICATE_EMAIL.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인 이메일과 같은 값은 중복 검사 없이 통과한다")
    void updateUser_sameEmailSkipsDuplicateCheck() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateUser(new UserUpdateCommand(
                USER_ID, ADMIN_ID, null, null, null, "owner01@example.com", "010-0000-0000", null, null));

        verify(userRepository, never()).existsByEmailAndUserIdNot(anyString(), any());
    }

    @Test
    @DisplayName("소속이 있는 점주를 승인하면 ACTIVE가 되고 이력을 남긴다")
    void updateUser_approve() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of(3L)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE));

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.USER, USER_ID,
                "PENDING", "ACTIVE", null, ADMIN_ID);
    }

    @Test
    @DisplayName("소속이 없는 창고 관리자·점주는 ACTIVE로 승인할 수 없다")
    void updateUser_approveWithoutAffiliation() {
        User user = userWith(USER_ID, UserRole.WAREHOUSE_MANAGER, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of()));

        assertErrorCode(() -> userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE)),
                AuthErrorCode.AFFILIATION_REQUIRED.name());
        verify(userRepository, never()).save(any());
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HQ_ADMIN은 소속 없이도 승인할 수 있다")
    void updateUser_approveHqAdminWithoutAffiliation() {
        User user = userWith(USER_ID, UserRole.HQ_ADMIN, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE));

        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userQueryRepository, never()).findAffiliation(any());
    }

    @Test
    @DisplayName("같은 요청에서 HQ_ADMIN으로 역할을 바꾸면 소속 없이 승인할 수 있다")
    void updateUser_approveWithRoleChangeToHqAdmin() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(new UserUpdateCommand(
                USER_ID, ADMIN_ID, null, null, null, null, null, UserRole.HQ_ADMIN, UserStatus.ACTIVE));

        assertThat(result.getRole()).isEqualTo(UserRole.HQ_ADMIN);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("가입 반려(PENDING→INACTIVE)는 소속 확인 없이 처리하고 이력을 남긴다")
    void updateUser_reject() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(statusUpdate(USER_ID, UserStatus.INACTIVE));

        assertThat(result.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userQueryRepository, never()).findAffiliation(any());
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.USER, USER_ID,
                "PENDING", "INACTIVE", null, ADMIN_ID);
    }

    @Test
    @DisplayName("활성 계정 비활성화와 비활성 계정 재활성화를 허용한다")
    void updateUser_deactivateAndReactivate() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of(3L)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(userService.updateUser(statusUpdate(USER_ID, UserStatus.INACTIVE)).getStatus())
                .isEqualTo(UserStatus.INACTIVE);
        assertThat(userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE)).getStatus())
                .isEqualTo(UserStatus.ACTIVE);

        verify(statusHistoryUseCase).record(StatusHistoryEntityType.USER, USER_ID,
                "ACTIVE", "INACTIVE", null, ADMIN_ID);
        verify(statusHistoryUseCase).record(StatusHistoryEntityType.USER, USER_ID,
                "INACTIVE", "ACTIVE", null, ADMIN_ID);
    }

    @Test
    @DisplayName("PENDING으로 되돌리거나 같은 상태로 바꾸면 INVALID_USER_STATUS_TRANSITION")
    void updateUser_invalidTransition() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));

        assertErrorCode(() -> userService.updateUser(statusUpdate(USER_ID, UserStatus.PENDING)),
                AuthErrorCode.INVALID_USER_STATUS_TRANSITION.name());
        assertErrorCode(() -> userService.updateUser(statusUpdate(USER_ID, UserStatus.ACTIVE)),
                AuthErrorCode.INVALID_USER_STATUS_TRANSITION.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("소속이 배정된 사용자의 역할 변경은 AFFILIATION_ASSIGNED")
    void updateUser_roleChangeWithAffiliationRejected() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of(3L)));

        assertErrorCode(() -> userService.updateUser(new UserUpdateCommand(
                        USER_ID, ADMIN_ID, null, null, null, null, null, UserRole.WAREHOUSE_MANAGER, null)),
                AuthErrorCode.AFFILIATION_ASSIGNED.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("소속이 없는 사용자의 역할은 바꿀 수 있고, 현재와 같은 역할은 소속 조회 없이 통과한다")
    void updateUser_roleChangeWithoutAffiliation() {
        User user = userWith(USER_ID, UserRole.STORE_OWNER, UserStatus.PENDING);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(), List.of()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User changed = userService.updateUser(new UserUpdateCommand(
                USER_ID, ADMIN_ID, null, null, null, null, null, UserRole.WAREHOUSE_MANAGER, null));
        assertThat(changed.getRole()).isEqualTo(UserRole.WAREHOUSE_MANAGER);

        userService.updateUser(new UserUpdateCommand(
                USER_ID, ADMIN_ID, null, null, null, null, null, UserRole.WAREHOUSE_MANAGER, null));
        verify(userQueryRepository, org.mockito.Mockito.times(1)).findAffiliation(USER_ID);
    }

    @Test
    @DisplayName("본인 계정의 역할이나 상태를 바꾸는 요청은 거절한다")
    void updateUser_selfRoleOrStatusRejected() {
        User admin = userWith(ADMIN_ID, UserRole.HQ_ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertErrorCode(() -> userService.updateUser(statusUpdate(ADMIN_ID, UserStatus.INACTIVE)),
                ErrorCode.VALIDATION_ERROR.name());
        assertErrorCode(() -> userService.updateUser(new UserUpdateCommand(
                        ADMIN_ID, ADMIN_ID, null, null, null, null, null, UserRole.STORE_OWNER, null)),
                ErrorCode.VALIDATION_ERROR.name());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("본인 계정이라도 이름·연락처 수정과 현재와 같은 역할·상태 값은 허용한다")
    void updateUser_selfProfileUpdate() {
        User admin = userWith(ADMIN_ID, UserRole.HQ_ADMIN, UserStatus.ACTIVE);
        when(userRepository.findByIdForUpdate(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(new UserUpdateCommand(
                ADMIN_ID, ADMIN_ID, null, null, "새이름", null, null, UserRole.HQ_ADMIN, UserStatus.ACTIVE));

        assertThat(result.getName()).isEqualTo("새이름");
        verify(statusHistoryUseCase, never()).record(any(), any(), any(), any(), any(), any());
    }
}
