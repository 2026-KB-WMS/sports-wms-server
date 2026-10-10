package com.kb.wms.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    private static final Long USER_ID = 12L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserQueryRepository userQueryRepository;
    @InjectMocks
    private AuthenticatedUserService service;

    private User userWith(UserRole role, UserStatus status) {
        return User.builder().userId(USER_ID).loginId("user01").passwordHash("hashed")
                .name("홍길동").email("user01@example.com").phone("010-1234-5678")
                .role(role).status(status).build();
    }

    @Test
    @DisplayName("ACTIVE 사용자는 DB의 역할과 현재 소속으로 인증 주체를 만든다")
    void resolve_active() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(UserRole.WAREHOUSE_MANAGER, UserStatus.ACTIVE)));
        when(userQueryRepository.findAffiliation(USER_ID)).thenReturn(new UserAffiliation(List.of(1L, 2L), List.of()));

        Optional<AuthenticatedUser> resolved = service.resolve(USER_ID);

        assertThat(resolved).contains(
                new AuthenticatedUser(USER_ID, UserRole.WAREHOUSE_MANAGER, List.of(1L, 2L), List.of()));
    }

    @Test
    @DisplayName("소속이 바뀌면 같은 사용자 ID라도 다음 호출에서 바뀐 소속을 돌려준다")
    void resolve_reflectsAffiliationChange() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(UserRole.STORE_OWNER, UserStatus.ACTIVE)));
        when(userQueryRepository.findAffiliation(USER_ID))
                .thenReturn(new UserAffiliation(List.of(), List.of(3L)))
                .thenReturn(new UserAffiliation(List.of(), List.of()));

        assertThat(service.resolve(USER_ID).orElseThrow().storeIds()).containsExactly(3L);
        assertThat(service.resolve(USER_ID).orElseThrow().storeIds()).isEmpty();
    }

    @Test
    @DisplayName("본사 관리자는 소속 조회 없이 빈 소속으로 만든다")
    void resolve_hqAdminSkipsAffiliation() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWith(UserRole.HQ_ADMIN, UserStatus.ACTIVE)));

        Optional<AuthenticatedUser> resolved = service.resolve(USER_ID);

        assertThat(resolved).contains(new AuthenticatedUser(USER_ID, UserRole.HQ_ADMIN, List.of(), List.of()));
        verify(userQueryRepository, never()).findAffiliation(any());
    }

    @Test
    @DisplayName("PENDING·INACTIVE 계정은 인증 주체를 만들지 않는다")
    void resolve_notActive() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(userWith(UserRole.STORE_OWNER, UserStatus.INACTIVE)))
                .thenReturn(Optional.of(userWith(UserRole.STORE_OWNER, UserStatus.PENDING)));

        assertThat(service.resolve(USER_ID)).isEmpty();
        assertThat(service.resolve(USER_ID)).isEmpty();
        verify(userQueryRepository, never()).findAffiliation(any());
    }

    @Test
    @DisplayName("사용자가 없으면 비어 있다")
    void resolve_notFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.resolve(USER_ID)).isEmpty();
    }
}
