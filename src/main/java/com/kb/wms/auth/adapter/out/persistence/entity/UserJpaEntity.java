package com.kb.wms.auth.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;
import com.kb.wms.common.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 테이블. user는 H2·PostgreSQL 예약어라 테이블명을 users로 쓴다.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        },
        indexes = @Index(name = "idx_users_role_status", columnList = "role, status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, length = 50)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    private UserJpaEntity(Long userId, String loginId, String passwordHash, String name, String email,
                          String phone, UserRole role, UserStatus status, LocalDateTime lastLoginAt) {
        this.userId = userId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.lastLoginAt = lastLoginAt;
    }

    public static UserJpaEntity fromDomain(User user) {
        return UserJpaEntity.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .passwordHash(user.getPasswordHash())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    public User toDomain() {
        return User.builder()
                .userId(userId)
                .loginId(loginId)
                .passwordHash(passwordHash)
                .name(name)
                .email(email)
                .phone(phone)
                .role(role)
                .status(status)
                .lastLoginAt(lastLoginAt)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
