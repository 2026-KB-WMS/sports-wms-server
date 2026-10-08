package com.kb.wms.auth.adapter.in.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.adapter.out.persistence.repository.UserJpaRepository;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * 설정(wms.admin.*)이 있으면 앱이 뜰 때 첫 본사 관리자가 만들어지고, 그 계정으로 로그인해 본사 전용 API를 쓸 수 있는지
 * 서버 전체 경로로 확인한다. 이 컨텍스트는 시작 시점에 관리자가 없는 빈 DB로 시작한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트와 인메모리 DB가 섞이지 않도록 전용 DB를 쓴다(Category.depth가 MySQL 전용 타입이라 MySQL 모드).
        "spring.datasource.url=jdbc:h2:mem:initial_admin;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "wms.admin.login-id=Init_Admin01",
        "wms.admin.password=P@ssw0rd!",
        "wms.admin.name=최초 관리자",
        "wms.admin.email=init_admin01@example.com",
        "wms.admin.phone=010-0000-0000"
})
class InitialHqAdminIntegrationTest {

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private InitialHqAdminRunner runner;

    private long hqAdminCount() {
        return userJpaRepository.findAll().stream().filter(user -> user.getRole() == UserRole.HQ_ADMIN).count();
    }

    @Test
    @DisplayName("기동 시 설정값으로 ACTIVE 본사 관리자가 만들어지고 비밀번호는 평문으로 저장되지 않는다")
    void createdAtStartup() {
        var admin = userJpaRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.HQ_ADMIN).findFirst().orElseThrow();

        assertThat(admin.getLoginId()).isEqualTo("init_admin01");
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getPasswordHash()).isNotEqualTo("P@ssw0rd!").startsWith("$2");
    }

    @Test
    @DisplayName("만들어진 관리자로 로그인해 본사 전용 API를 호출할 수 있다")
    void canLoginAndUseAdminApi() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\": \"init_admin01\", \"password\": \"P@ssw0rd!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("HQ_ADMIN"))
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = ACCESS_TOKEN.matcher(response);
        assertThat(matcher.find()).isTrue();

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + matcher.group(1)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("다시 실행해도 관리자는 한 명뿐이다 (재시작해도 안전)")
    void runningAgainDoesNotCreateAnother() {
        runner.run(null);
        runner.run(null);

        assertThat(hqAdminCount()).isEqualTo(1);
    }
}
