package com.kb.wms.auth.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    @DisplayName("HQ_ADMIN만 가입 신청으로 선택할 수 없다")
    void selfSignupAllowed() {
        assertThat(UserRole.HQ_ADMIN.isSelfSignupAllowed()).isFalse();
        assertThat(UserRole.WAREHOUSE_MANAGER.isSelfSignupAllowed()).isTrue();
        assertThat(UserRole.STORE_OWNER.isSelfSignupAllowed()).isTrue();
    }

    @Test
    @DisplayName("창고 관리자·점주는 소속이 필요하고 본사 관리자는 필요 없다")
    void requiresAffiliation() {
        assertThat(UserRole.HQ_ADMIN.requiresAffiliation()).isFalse();
        assertThat(UserRole.WAREHOUSE_MANAGER.requiresAffiliation()).isTrue();
        assertThat(UserRole.STORE_OWNER.requiresAffiliation()).isTrue();
    }
}
