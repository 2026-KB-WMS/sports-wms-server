package com.kb.wms.auth.domain.entity;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인·권한·계정 상태를 관리하는 회원.
 * 가입 신청 계정은 PENDING으로 만들어지고, 본사 관리자가 승인(ACTIVE)해야 로그인할 수 있다.
 * 창고·지점 소속은 WarehouseMember/StoreMember가 가지며 이 엔티티에는 두지 않는다.
 *
 * <p>입력 형식 검증(아이디·비밀번호 규칙)과 소속 배정 여부 검사는 서비스 계층의 책임이고,
 * 여기서는 상태 전이 같은 도메인 불변식만 지킨다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    private Long userId;
    private String loginId;
    private String passwordHash;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    private User(Long userId, String loginId, String passwordHash, String name, String email,
                 String phone, UserRole role, UserStatus status, LocalDateTime lastLoginAt,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status == null ? UserStatus.PENDING : status;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 가입 신청. PENDING으로 생성하며 HQ_ADMIN 역할은 가입으로 만들 수 없다. */
    public static User signUp(String loginId, String passwordHash, String name, String email,
                              String phone, UserRole role) {
        requireText(loginId, "로그인 아이디는 필수입니다.");
        requireText(passwordHash, "비밀번호는 필수입니다.");
        requireText(name, "이름은 필수입니다.");
        requireText(email, "이메일은 필수입니다.");
        requireText(phone, "연락처는 필수입니다.");
        if (role == null) {
            throw new IllegalArgumentException("역할은 필수입니다.");
        }
        if (!role.isSelfSignupAllowed()) {
            throw new IllegalArgumentException("가입으로 만들 수 없는 역할입니다: " + role);
        }
        return User.builder()
                .loginId(loginId)
                .passwordHash(passwordHash)
                .name(name)
                .email(email)
                .phone(phone)
                .role(role)
                .status(UserStatus.PENDING)
                .build();
    }

    /** 최초 본사 관리자 생성(시작 시 초기화용). 승인 절차 없이 ACTIVE로 만든다. */
    public static User createHqAdmin(String loginId, String passwordHash, String name, String email,
                                     String phone) {
        requireText(loginId, "로그인 아이디는 필수입니다.");
        requireText(passwordHash, "비밀번호는 필수입니다.");
        requireText(name, "이름은 필수입니다.");
        requireText(email, "이메일은 필수입니다.");
        requireText(phone, "연락처는 필수입니다.");
        return User.builder()
                .loginId(loginId)
                .passwordHash(passwordHash)
                .name(name)
                .email(email)
                .phone(phone)
                .role(UserRole.HQ_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // loginId와 비밀번호는 가입 후 수정할 수 없으므로 변경 메서드를 두지 않는다.

    public void changeName(String name) {
        requireText(name, "이름은 필수입니다.");
        this.name = name;
    }

    public void changeEmail(String email) {
        requireText(email, "이메일은 필수입니다.");
        this.email = email;
    }

    public void changePhone(String phone) {
        requireText(phone, "연락처는 필수입니다.");
        this.phone = phone;
    }

    public void changeRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("역할은 필수입니다.");
        }
        this.role = role;
    }

    /** 가입 승인: PENDING → ACTIVE. 소속 배정 여부는 서비스에서 먼저 검사한다. */
    public void approve() {
        requireStatus(UserStatus.PENDING, "가입 승인은 PENDING 상태에서만 가능합니다.");
        this.status = UserStatus.ACTIVE;
    }

    /** 가입 반려: PENDING → INACTIVE. */
    public void reject() {
        requireStatus(UserStatus.PENDING, "가입 반려는 PENDING 상태에서만 가능합니다.");
        this.status = UserStatus.INACTIVE;
    }

    /** 비활성화: ACTIVE → INACTIVE. */
    public void deactivate() {
        requireStatus(UserStatus.ACTIVE, "비활성화는 ACTIVE 상태에서만 가능합니다.");
        this.status = UserStatus.INACTIVE;
    }

    /** 재활성화: INACTIVE → ACTIVE. */
    public void reactivate() {
        requireStatus(UserStatus.INACTIVE, "재활성화는 INACTIVE 상태에서만 가능합니다.");
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 목표 상태로 전이한다 (PATCH /users/{userId}의 status 처리용).
     * 현재 상태에서 허용되지 않는 전이(같은 상태로의 변경, PENDING으로 되돌리기)는 예외.
     */
    public void changeStatus(UserStatus target) {
        if (target == null) {
            throw new IllegalArgumentException("상태는 필수입니다.");
        }
        switch (target) {
            case ACTIVE -> {
                if (this.status == UserStatus.PENDING) {
                    approve();
                } else {
                    reactivate();
                }
            }
            case INACTIVE -> {
                if (this.status == UserStatus.PENDING) {
                    reject();
                } else {
                    deactivate();
                }
            }
            case PENDING -> throw new IllegalStateException("PENDING 상태로 되돌릴 수 없습니다.");
        }
    }

    /** {@link #changeStatus}가 성공하는 전이인가. 같은 상태로의 변경과 PENDING으로 되돌리기는 불가. */
    public boolean canChangeStatusTo(UserStatus target) {
        return target != null && target != UserStatus.PENDING && target != this.status;
    }

    /** 로그인 성공 시각을 기록한다. */
    public void recordLogin(LocalDateTime loggedInAt) {
        this.lastLoginAt = loggedInAt;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isHqAdmin() {
        return this.role == UserRole.HQ_ADMIN;
    }

    private void requireStatus(UserStatus expected, String message) {
        if (this.status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
