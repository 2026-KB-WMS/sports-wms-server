package com.kb.wms.auth.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.AuthenticatedUserResolver;

import lombok.RequiredArgsConstructor;

/**
 * 요청마다 사용자의 역할·상태·소속을 DB에서 읽어 인증 주체를 만든다(ADR-016).
 * 본사 관리자는 소속이 없으므로 소속 조회를 건너뛴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthenticatedUserService implements AuthenticatedUserResolver {

    private final UserRepository userRepository;
    private final UserQueryRepository userQueryRepository;

    @Override
    public Optional<AuthenticatedUser> resolve(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(this::toAuthenticatedUser);
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        if (user.getRole() == UserRole.HQ_ADMIN) {
            return new AuthenticatedUser(user.getUserId(), user.getRole(), List.of(), List.of());
        }
        UserAffiliation affiliation = userQueryRepository.findAffiliation(user.getUserId());
        return new AuthenticatedUser(user.getUserId(), user.getRole(),
                affiliation.warehouseIds(), affiliation.storeIds());
    }
}
