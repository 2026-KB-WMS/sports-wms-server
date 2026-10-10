package com.kb.wms.auth.application.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.AuthUseCase;
import com.kb.wms.auth.application.port.in.command.LoginCommand;
import com.kb.wms.auth.application.port.in.result.LoginResult;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.exception.AuthErrorCode;
import com.kb.wms.common.exception.BusinessException;
import com.kb.wms.common.exception.ErrorCode;
import com.kb.wms.common.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService implements AuthUseCase {

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private volatile String dummyHash;

    @Override
    @Transactional
    public LoginResult login(LoginCommand command) {
        if (isBlank(command.loginId()) || isBlank(command.password())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "로그인 아이디와 비밀번호를 입력해주세요.");
        }

        // 가입 때 소문자로 통일해 저장하므로 같은 형식으로 맞춰 조회한다.
        User found = userRepository.findByLoginId(command.loginId().trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (found == null) {
            // 없는 아이디도 해시 비교만큼 걸리게 해서 응답 시간으로 계정 존재 여부가 드러나지 않게 한다.
            passwordEncoder.matches(command.password(), dummyHash());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }
        if (!passwordEncoder.matches(command.password(), found.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        // 해시 비교하는 동안 관리자가 상태를 바꿨을 수 있다. 행을 잠그고 다시 읽어, 낡은 값으로 덮어쓰지 않고 최신 상태로 판단한다.
        User user = userRepository.findByIdForUpdate(found.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE));

        // 상태는 비밀번호가 맞은 뒤에만 알려 준다(비밀번호를 모르는 사람에게 계정 상태를 노출하지 않는다).
        switch (user.getStatus()) {
            case PENDING -> throw new BusinessException(AuthErrorCode.ACCOUNT_PENDING);
            case INACTIVE -> throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            case ACTIVE -> { }
        }

        user.recordLogin(LocalDateTime.now());
        User saved = userRepository.save(user);

        String accessToken = jwtProvider.createAccessToken(saved.getUserId());
        return new LoginResult(accessToken, jwtProvider.getAccessTokenValiditySeconds(), saved);
    }

    private String dummyHash() {
        String hash = dummyHash;
        if (hash == null) {
            hash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
            dummyHash = hash;
        }
        return hash;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
