package com.kb.wms.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.adapter.out.persistence.repository.UserJpaRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * 가입 → 로그인 → 토큰으로 API 호출까지 서버 전체 경로(보안 필터, 컨트롤러, 서비스, DB)에서 확인한다.
 * 계정 상태별 로그인 결과(PENDING·INACTIVE는 403)와 토큰 역할에 따른 접근 제한을 본다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트와 인메모리 DB가 섞이지 않도록 전용 DB를 쓴다(Category.depth가 MySQL 전용 타입이라 MySQL 모드).
        "spring.datasource.url=jdbc:h2:mem:auth_integration;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class AuthFlowIntegrationTest {

    private static final String LOGIN_ID = "flow_owner01";
    private static final String PASSWORD = "P@ssw0rd!";
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;

    @AfterEach
    void tearDown() {
        userJpaRepository.deleteAll(userJpaRepository.findAll().stream()
                .filter(user -> user.getLoginId().startsWith("flow_")).toList());
    }

    private void signUp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "loginId": "%s",
                          "password": "%s",
                          "name": "흐름 점주",
                          "email": "flow_owner01@example.com",
                          "phone": "010-1234-5678",
                          "role": "STORE_OWNER"
                        }
                        """.formatted(LOGIN_ID, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\": \"%s\", \"password\": \"%s\"}".formatted(LOGIN_ID, password)));
    }

    private void changeStatus(UserStatus target) {
        User user = userRepository.findByLoginId(LOGIN_ID).orElseThrow();
        user.changeStatus(target);
        userRepository.save(user);
    }

    private String loginAndGetToken() throws Exception {
        String response = login(PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Matcher matcher = ACCESS_TOKEN.matcher(response);
        assertThat(matcher.find()).as("로그인 응답에 accessToken이 있다: %s", response).isTrue();
        return matcher.group(1);
    }

    @Test
    @DisplayName("승인 대기(PENDING) 계정은 올바른 비밀번호여도 403 ACCOUNT_PENDING이고 토큰을 받지 못한다")
    void pendingAccountCannotLogin() throws Exception {
        signUp();

        login(PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("비활성(INACTIVE) 계정은 403 ACCOUNT_INACTIVE이고, 다시 활성화하면 로그인할 수 있다")
    void inactiveAccountCannotLogin_untilReactivated() throws Exception {
        signUp();
        changeStatus(UserStatus.INACTIVE);

        login(PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"));

        changeStatus(UserStatus.ACTIVE);

        login(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 계정 상태와 상관없이 401이라 PENDING 여부가 드러나지 않는다")
    void wrongPasswordIs401_evenForPending() throws Exception {
        signUp();

        login("Wrong-P@ss1")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("발급받은 점주 토큰으로 내 정보는 조회되고, 본사 전용 API는 403이다")
    void issuedTokenWorksUntilRoleMismatch() throws Exception {
        signUp();
        changeStatus(UserStatus.ACTIVE);
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value(LOGIN_ID));
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("이미 발급된 토큰도 계정이 비활성화되면 다음 요청부터 401이고, 다시 활성화하면 같은 토큰이 통한다 (ADR-016)")
    void issuedTokenRejectedAfterDeactivation() throws Exception {
        signUp();
        changeStatus(UserStatus.ACTIVE);
        String token = loginAndGetToken();

        changeStatus(UserStatus.INACTIVE);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
        login(PASSWORD).andExpect(status().isForbidden());

        changeStatus(UserStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
