package com.kb.wms.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    private User pending() {
        return User.signUp("store_owner01", "hashed", "김점주", "owner01@example.com",
                "010-1234-5678", UserRole.STORE_OWNER);
    }

    private User active() {
        User u = pending();
        u.approve();
        return u;
    }

    private User inactive() {
        User u = active();
        u.deactivate();
        return u;
    }

    @Test
    @DisplayName("가입 신청은 PENDING 상태로 생성된다")
    void signUp() {
        User u = pending();

        assertThat(u.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(u.getRole()).isEqualTo(UserRole.STORE_OWNER);
        assertThat(u.getLastLoginAt()).isNull();
        assertThat(u.isActive()).isFalse();
        assertThat(u.isHqAdmin()).isFalse();
    }

    @Test
    @DisplayName("HQ_ADMIN 역할은 가입으로 만들 수 없다")
    void signUpHqAdminRejected() {
        assertThatThrownBy(() -> User.signUp("hq01", "hashed", "홍길동", "hq@example.com",
                "010-0000-0000", UserRole.HQ_ADMIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("필수 값이 비어 있으면 가입 실패")
    void signUpInvalid() {
        assertThatThrownBy(() -> User.signUp(" ", "h", "이름", "a@b.com", "010", UserRole.STORE_OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.signUp("id", null, "이름", "a@b.com", "010", UserRole.STORE_OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.signUp("id", "h", "", "a@b.com", "010", UserRole.STORE_OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.signUp("id", "h", "이름", null, "010", UserRole.STORE_OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.signUp("id", "h", "이름", "a@b.com", " ", UserRole.STORE_OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.signUp("id", "h", "이름", "a@b.com", "010", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최초 본사 관리자는 승인 없이 ACTIVE로 만들어진다")
    void createHqAdmin() {
        User u = User.createHqAdmin("hq_admin01", "hashed", "홍길동", "admin@example.com", "010-1111-2222");

        assertThat(u.getRole()).isEqualTo(UserRole.HQ_ADMIN);
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.isActive()).isTrue();
        assertThat(u.isHqAdmin()).isTrue();
    }

    @Test
    @DisplayName("비밀번호 해시를 바꾸고, 빈 값은 거절한다")
    void changePasswordHash() {
        User u = User.createHqAdmin("hq_admin01", "hashed", "홍길동", "admin@example.com", "010-1111-2222");

        u.changePasswordHash("new-hashed");

        assertThat(u.getPasswordHash()).isEqualTo("new-hashed");
        assertThatThrownBy(() -> u.changePasswordHash(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(u.getPasswordHash()).isEqualTo("new-hashed");
    }

    @Test
    @DisplayName("빌더로 복원할 때 상태가 없으면 PENDING이 기본값")
    void builderDefaultStatus() {
        User u = User.builder().loginId("id").build();

        assertThat(u.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("승인: PENDING → ACTIVE")
    void approve() {
        User u = pending();
        u.approve();

        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.isActive()).isTrue();
    }

    @Test
    @DisplayName("가입 반려: PENDING → INACTIVE")
    void reject() {
        User u = pending();
        u.reject();

        assertThat(u.getStatus()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("비활성화: ACTIVE → INACTIVE, 재활성화: INACTIVE → ACTIVE")
    void deactivateAndReactivate() {
        User u = active();

        u.deactivate();
        assertThat(u.getStatus()).isEqualTo(UserStatus.INACTIVE);

        u.reactivate();
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("잘못된 상태에서의 전이는 예외")
    void invalidTransitions() {
        assertThatThrownBy(() -> active().approve()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> inactive().approve()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> active().reject()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending().deactivate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> inactive().deactivate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending().reactivate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> active().reactivate()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("changeStatus: 허용된 4가지 전이를 목표 상태로 처리한다")
    void changeStatusAllowed() {
        User approved = pending();
        approved.changeStatus(UserStatus.ACTIVE);
        assertThat(approved.getStatus()).isEqualTo(UserStatus.ACTIVE);

        User rejected = pending();
        rejected.changeStatus(UserStatus.INACTIVE);
        assertThat(rejected.getStatus()).isEqualTo(UserStatus.INACTIVE);

        User deactivated = active();
        deactivated.changeStatus(UserStatus.INACTIVE);
        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);

        User reactivated = inactive();
        reactivated.changeStatus(UserStatus.ACTIVE);
        assertThat(reactivated.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("changeStatus: 같은 상태로의 변경과 PENDING으로 되돌리기는 예외")
    void changeStatusRejected() {
        assertThatThrownBy(() -> active().changeStatus(UserStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> inactive().changeStatus(UserStatus.INACTIVE))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending().changeStatus(UserStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> active().changeStatus(UserStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending().changeStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("실패한 전이는 상태를 바꾸지 않는다")
    void failedTransitionKeepsStatus() {
        User u = active();

        assertThatThrownBy(() -> u.changeStatus(UserStatus.PENDING))
                .isInstanceOf(IllegalStateException.class);

        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("이름·이메일·연락처·역할 변경, 빈 값은 거절")
    void changeProfile() {
        User u = pending();

        u.changeName("박점주");
        u.changeEmail("new@example.com");
        u.changePhone("010-9876-5432");
        u.changeRole(UserRole.WAREHOUSE_MANAGER);

        assertThat(u.getName()).isEqualTo("박점주");
        assertThat(u.getEmail()).isEqualTo("new@example.com");
        assertThat(u.getPhone()).isEqualTo("010-9876-5432");
        assertThat(u.getRole()).isEqualTo(UserRole.WAREHOUSE_MANAGER);

        assertThatThrownBy(() -> u.changeName(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> u.changeEmail(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> u.changePhone("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> u.changeRole(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상태 변경 가능 여부는 changeStatus가 허용하는 전이와 같다")
    void canChangeStatusTo() {
        assertThat(pending().canChangeStatusTo(UserStatus.ACTIVE)).isTrue();
        assertThat(pending().canChangeStatusTo(UserStatus.INACTIVE)).isTrue();
        assertThat(active().canChangeStatusTo(UserStatus.INACTIVE)).isTrue();
        assertThat(inactive().canChangeStatusTo(UserStatus.ACTIVE)).isTrue();

        assertThat(pending().canChangeStatusTo(UserStatus.PENDING)).isFalse();
        assertThat(active().canChangeStatusTo(UserStatus.ACTIVE)).isFalse();
        assertThat(active().canChangeStatusTo(UserStatus.PENDING)).isFalse();
        assertThat(inactive().canChangeStatusTo(UserStatus.INACTIVE)).isFalse();
        assertThat(active().canChangeStatusTo(null)).isFalse();
    }

    @Test
    @DisplayName("로그인 시각 기록")
    void recordLogin() {
        User u = active();
        u.recordLogin(NOW);

        assertThat(u.getLastLoginAt()).isEqualTo(NOW);
    }
}
