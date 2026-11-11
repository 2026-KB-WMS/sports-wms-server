package com.kb.wms.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiTimingFilterAbbreviateTest {

    @Test
    @DisplayName("컬럼 목록이 긴 select는 select ... from으로 줄인다")
    void abbreviatesLongSelectList() {
        String sql = "select u.user_id,u.created_at,u.email,u.last_login_at,u.login_id,u.name,u.password_hash"
                + " from users u where u.user_id=?";

        assertThat(ApiTimingFilter.abbreviateSelect(sql)).isEqualTo("select ... from users u where u.user_id=?");
    }

    @Test
    @DisplayName("짧은 select 목록은 집계 내용을 알 수 있게 그대로 둔다")
    void keepsShortSelectList() {
        String sql = "select count(distinct s.supplier_order_id) from supplier_order s where s.status=?";

        assertThat(ApiTimingFilter.abbreviateSelect(sql)).isEqualTo(sql);
    }

    @Test
    @DisplayName("select로 시작하지 않는 문장은 그대로 둔다")
    void keepsNonSelect() {
        String sql = "update users set last_login_at=? where user_id=?";

        assertThat(ApiTimingFilter.abbreviateSelect(sql)).isEqualTo(sql);
    }
}
