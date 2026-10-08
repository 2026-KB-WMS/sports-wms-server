package com.kb.wms.auth.adapter.in.startup;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.TestPropertySource;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;
import com.kb.wms.auth.adapter.out.persistence.repository.UserJpaRepository;
import com.kb.wms.auth.application.port.in.UserUseCase;
import com.kb.wms.auth.domain.enums.UserRole;

/**
 * 서버가 여러 대 동시에 떠서 모두 첫 관리자를 만들려 해도 한 명만 만들어지고, 어느 쪽도 예외로 기동이 깨지지 않는지 본다.
 * "관리자 없음 확인 후 저장" 순서라 동시 실행이 확인을 함께 통과할 수 있으므로, 최종 방어선인 DB 유니크 제약
 * (uk_users_login_id, uk_users_email)을 실제 H2로 검증한다.
 *
 * <p>테스트 메서드는 트랜잭션을 걸지 않는다(실행마다 자신의 트랜잭션·커넥션을 얻어야 실제 동시성이 재현된다).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        // 다른 통합 테스트 컨텍스트와 인메모리 DB가 섞이지 않도록 전용 DB를 쓴다(Category.depth가 MySQL 전용 타입이라 MySQL 모드).
        "spring.datasource.url=jdbc:h2:mem:initial_admin_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class InitialHqAdminConcurrencyTest {

    private static final int THREADS = 8;

    @Autowired
    private UserUseCase userUseCase;
    @Autowired
    private UserJpaRepository userJpaRepository;

    @AfterEach
    void tearDown() {
        List<UserJpaEntity> admins = userJpaRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.HQ_ADMIN).toList();
        userJpaRepository.deleteAll(admins);
    }

    private long hqAdminCount() {
        return userJpaRepository.findAll().stream().filter(user -> user.getRole() == UserRole.HQ_ADMIN).count();
    }

    @Test
    @DisplayName("동시에 여러 인스턴스가 첫 관리자를 만들려 해도 한 명만 만들어지고 예외로 끝나는 실행은 없다")
    void onlyOneAdminIsCreated() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Throwable>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                InitialHqAdminRunner runner = new InitialHqAdminRunner(userUseCase,
                        "conc_admin01", "P@ssw0rd!", "동시 관리자", "conc_admin01@example.com", "010-0000-0000");
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        runner.run(null);
                        return null;
                    } catch (Throwable e) {
                        return e;
                    }
                }));
            }
            ready.await(30, TimeUnit.SECONDS);
            start.countDown();
            for (Future<Throwable> future : futures) {
                assertThat(future.get(60, TimeUnit.SECONDS)).as("러너는 어떤 경우에도 예외를 밖으로 내보내지 않는다").isNull();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(hqAdminCount()).isEqualTo(1);
    }
}
