package com.kb.wms.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.application.port.in.command.InitialHqAdminCommand;
import com.kb.wms.auth.application.port.in.command.UserSignupCommand;
import com.kb.wms.auth.application.port.in.command.UserUpdateCommand;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.application.port.in.result.InitialHqAdminResult;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserUseCase {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z0-9_]{4,20}$");
    // 영문, 숫자, 특수문자(영문·숫자·공백이 아닌 문자)를 각각 하나 이상 포함하는 8자 이상
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,}$");
    // BCrypt는 72바이트를 넘는 입력을 처리하지 못한다.
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;
    private final StatusHistoryUseCase statusHistoryUseCase;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User signUp(UserSignupCommand command) {
        if (command.role() == null || !command.role().isSelfSignupAllowed()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "가입할 수 없는 역할입니다.");
        }
        String loginId = normalizeLoginId(command.loginId());
        validateLoginId(loginId);
        validatePassword(command.password());

        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.signUp(loginId, passwordEncoder.encode(command.password()),
                command.name(), command.email(), command.phone(), command.role());
        return userRepository.save(user);
    }

    @Override
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<User> getUsers(UserSearchCondition condition) {
        return userRepository.search(condition);
    }

    @Override
    @Transactional
    public User updateUser(UserUpdateCommand command) {
        if (command.loginId() != null || command.password() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "로그인 아이디와 비밀번호는 수정할 수 없습니다.");
        }
        if (command.hasNoChanges()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "수정할 필드를 하나 이상 입력해주세요.");
        }

        User user = userRepository.findByIdForUpdate(command.userId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        UserRole newRole = command.role();
        UserStatus newStatus = command.status();
        if (user.getUserId().equals(command.actorUserId())) {
            // 마지막 관리자 계정이 잠기지 않도록 본인의 역할·상태는 바꿀 수 없다. 같은 값은 변경이 아니므로 무시한다.
            if ((newRole != null && newRole != user.getRole())
                    || (newStatus != null && newStatus != user.getStatus())) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "본인 계정의 역할과 상태는 변경할 수 없습니다.");
            }
            newRole = null;
            newStatus = null;
        }

        if (command.email() != null && !command.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndUserIdNot(command.email(), user.getUserId())) {
                throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
            }
            user.changeEmail(command.email());
        }
        if (command.name() != null) {
            user.changeName(command.name());
        }
        if (command.phone() != null) {
            user.changePhone(command.phone());
        }

        if (newRole != null && newRole != user.getRole()
                && userQueryRepository.findAffiliation(user.getUserId()).hasAny()) {
            throw new BusinessException(AuthErrorCode.AFFILIATION_ASSIGNED);
        }

        UserStatus fromStatus = user.getStatus();
        if (newStatus != null) {
            validateStatusChange(user, newStatus, newRole == null ? user.getRole() : newRole);
        }
        if (newRole != null) {
            user.changeRole(newRole);
        }
        if (newStatus != null) {
            user.changeStatus(newStatus);
        }

        User saved = userRepository.save(user);
        if (newStatus != null) {
            statusHistoryUseCase.record(StatusHistoryEntityType.USER, saved.getUserId(),
                    fromStatus.name(), newStatus.name(), null, command.actorUserId());
        }
        return saved;
    }

    @Override
    @Transactional
    public InitialHqAdminResult ensureInitialHqAdmin(InitialHqAdminCommand command) {
        if (userRepository.existsByRole(UserRole.HQ_ADMIN)) {
            return InitialHqAdminResult.ALREADY_EXISTS;
        }
        if (command.isEmpty()) {
            return InitialHqAdminResult.NOT_CONFIGURED;
        }
        validateInitialAdminProfile(command);
        String loginId = normalizeLoginId(command.loginId());
        validateLoginId(loginId);
        validatePassword(command.password());

        if (userRepository.existsByLoginId(loginId)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        userRepository.save(User.createHqAdmin(loginId, passwordEncoder.encode(command.password()),
                command.name(), command.email(), command.phone()));
        return InitialHqAdminResult.CREATED;
    }

    private void validateInitialAdminProfile(InitialHqAdminCommand command) {
        List<String> missing = new ArrayList<>();
        if (isBlank(command.loginId())) {
            missing.add("loginId");
        }
        if (isBlank(command.password())) {
            missing.add("password");
        }
        if (isBlank(command.name())) {
            missing.add("name");
        }
        if (isBlank(command.email())) {
            missing.add("email");
        }
        if (isBlank(command.phone())) {
            missing.add("phone");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "최초 본사 관리자 설정에 비어 있는 값이 있습니다: " + String.join(", ", missing));
        }
        // 컬럼 길이를 넘으면 DB 오류가 나서 동시 기동 중복과 구분되지 않으므로 미리 거른다.
        if (command.name().length() > 100 || command.email().length() > 255 || command.phone().length() > 30
                || !command.email().contains("@")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "최초 본사 관리자의 이름(100자)·이메일(255자, @ 포함)·연락처(30자) 형식을 확인해주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateStatusChange(User user, UserStatus target, UserRole roleAfterChange) {
        if (!user.canChangeStatusTo(target)) {
            throw new BusinessException(AuthErrorCode.INVALID_USER_STATUS_TRANSITION,
                    "허용되지 않는 계정 상태 변경입니다. " + user.getStatus() + " → " + target);
        }
        if (target == UserStatus.ACTIVE && roleAfterChange.requiresAffiliation()
                && !userQueryRepository.findAffiliation(user.getUserId()).hasAny()) {
            throw new BusinessException(AuthErrorCode.AFFILIATION_REQUIRED);
        }
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? null : loginId.trim().toLowerCase(Locale.ROOT);
    }

    private void validateLoginId(String loginId) {
        if (loginId == null || !LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "로그인 아이디는 영문 소문자, 숫자, 밑줄(_)로 4~20자여야 합니다.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "비밀번호가 너무 깁니다.");
        }
    }
}
