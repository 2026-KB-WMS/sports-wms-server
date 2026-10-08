package com.kb.wms.auth.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;
import com.kb.wms.auth.adapter.out.persistence.repository.UserJpaRepository;

/**
 * 같은 로그인 아이디·이메일로 동시에 가입을 요청해도 1건만 만들어지고 나머지는 409로 거절되는지 본다.
 * 가입 서비스는 "중복 확인 후 저장" 순서라 동시 요청이 확인을 함께 통과할 수 있으므로, 최종 방어선인 DB 유니크 제약
 * (uk_users_login_id, uk_users_email)과 그 오류의 409 변환까지 서버 전체 경로로 검증한다.
 *
 * <p>테스트 메서드는 트랜잭션을 걸지 않는다(요청마다 자신의 트랜잭션·커넥션을 얻어야 실제 동시성이 재현된다).
 * 대신 각 테스트가 만든 계정을 {@code @AfterEach}에서 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트와 인메모리 DB가 섞이지 않도록 전용 DB를 쓴다(Category.depth가 MySQL 전용 타입이라 MySQL 모드).
        "spring.datasource.url=jdbc:h2:mem:auth_integration;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class SignupConcurrencyTest {

    private static final String PREFIX = "conc_";
    private static final int THREADS = 8;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserJpaRepository userJpaRepository;

    @AfterEach
    void tearDown() {
        List<UserJpaEntity> created = userJpaRepository.findAll().stream()
                .filter(user -> user.getLoginId().startsWith(PREFIX)).toList();
        userJpaRepository.deleteAll(created);
    }

    private static String body(String loginId, String email) {
        return """
                {
                  "loginId": "%s",
                  "password": "P@ssw0rd!",
                  "name": "동시 가입",
                  "email": "%s",
                  "phone": "010-1234-5678",
                  "role": "STORE_OWNER"
                }
                """.formatted(loginId, email);
    }

    private long countCreated() {
        return userJpaRepository.findAll().stream().filter(user -> user.getLoginId().startsWith(PREFIX)).count();
    }

    /** 모든 요청이 준비된 뒤 한꺼번에 시작해 응답 상태 코드를 요청 순서대로 돌려준다. */
    private List<Integer> signUpConcurrently(List<String> bodies) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(bodies.size());
        try {
            CountDownLatch ready = new CountDownLatch(bodies.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> futures = new ArrayList<>();
            for (String body : bodies) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return mockMvc.perform(post("/api/v1/auth/signup")
                                    .contentType(MediaType.APPLICATION_JSON).content(body))
                            .andReturn().getResponse().getStatus();
                }));
            }
            ready.await(30, TimeUnit.SECONDS);
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private void assertOneCreatedRestConflict(List<Integer> statuses) {
        assertThat(statuses.stream().filter(status -> status == 201).count())
                .as("가입에 성공하는 요청은 정확히 1건이다: %s", statuses).isEqualTo(1);
        assertThat(statuses.stream().filter(status -> status != 201).toList())
                .as("나머지는 모두 409로 거절된다(500이 섞이면 유니크 위반 변환이 빠진 것): %s", statuses)
                .hasSize(THREADS - 1).allMatch(status -> status == 409);
        assertThat(countCreated()).as("저장된 계정은 1건이다").isEqualTo(1);
    }

    @Test
    @DisplayName("같은 로그인 아이디·이메일로 동시에 가입하면 1건만 성공하고 나머지는 409다")
    void sameLoginIdAndEmail() throws Exception {
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            bodies.add(body(PREFIX + "same_all", "conc_same_all@example.com"));
        }

        assertOneCreatedRestConflict(signUpConcurrently(bodies));
    }

    @Test
    @DisplayName("같은 로그인 아이디로 이메일만 다르게 동시에 가입해도 1건만 성공하고 나머지는 409다")
    void sameLoginIdDifferentEmail() throws Exception {
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            bodies.add(body(PREFIX + "same_login", "conc_login_" + i + "@example.com"));
        }

        assertOneCreatedRestConflict(signUpConcurrently(bodies));
    }

    @Test
    @DisplayName("같은 이메일로 로그인 아이디만 다르게 동시에 가입해도 1건만 성공하고 나머지는 409다")
    void sameEmailDifferentLoginId() throws Exception {
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            bodies.add(body(PREFIX + "email_" + i, "conc_same_email@example.com"));
        }

        assertOneCreatedRestConflict(signUpConcurrently(bodies));
    }
}
