package com.kb.wms.auth.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.auth.domain.enums.UserStatus;

/**
 * 회원 영속성 어댑터(저장·잠금 조회·중복 확인·목록 검색) 검증.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class UserPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    @Autowired UserRepository userRepository;

    private User signUp(String loginId, String name, String email, UserRole role) {
        return userRepository.save(User.signUp(loginId, "hashed", name, email, "010-1234-5678", role));
    }

    @Test
    @DisplayName("저장하면 ID와 생성 시각이 채워지고 다시 읽을 수 있다")
    void save() {
        User saved = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);

        User found = userRepository.findById(saved.getUserId()).orElseThrow();

        assertThat(saved.getUserId()).isNotNull();
        assertThat(found.getLoginId()).isEqualTo("store_owner01");
        assertThat(found.getPasswordHash()).isEqualTo("hashed");
        assertThat(found.getName()).isEqualTo("김점주");
        assertThat(found.getEmail()).isEqualTo("owner01@example.com");
        assertThat(found.getPhone()).isEqualTo("010-1234-5678");
        assertThat(found.getRole()).isEqualTo(UserRole.STORE_OWNER);
        assertThat(found.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(found.getLastLoginAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("상태 전이와 로그인 시각을 저장하면 반영된다")
    void update() {
        User saved = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        saved.approve();
        saved.recordLogin(NOW);

        userRepository.save(saved);

        User found = userRepository.findByIdForUpdate(saved.getUserId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.getLastLoginAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("수정 후 save가 돌려주는 사용자의 updatedAt은 갱신된 값이다")
    void saveReturnsRefreshedUpdatedAt() throws InterruptedException {
        User saved = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        Thread.sleep(20);
        saved.changeName("박점주");

        User updated = userRepository.save(saved);

        assertThat(updated.getUpdatedAt()).isAfter(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("없는 ID는 비어 있다")
    void notFound() {
        assertThat(userRepository.findById(999_999L)).isEmpty();
        assertThat(userRepository.findByIdForUpdate(999_999L)).isEmpty();
        assertThat(userRepository.findByLoginId("nobody")).isEmpty();
    }

    @Test
    @DisplayName("로그인 아이디로 조회한다")
    void findByLoginId() {
        User saved = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        signUp("store_owner02", "박점주", "owner02@example.com", UserRole.STORE_OWNER);

        User found = userRepository.findByLoginId("store_owner01").orElseThrow();

        assertThat(found.getUserId()).isEqualTo(saved.getUserId());
    }

    @Test
    @DisplayName("아이디·이메일 중복 여부와 본인 제외 이메일 중복 확인")
    void existsChecks() {
        User owner = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        User other = signUp("store_owner02", "박점주", "owner02@example.com", UserRole.STORE_OWNER);

        assertThat(userRepository.existsByLoginId("store_owner01")).isTrue();
        assertThat(userRepository.existsByLoginId("store_owner99")).isFalse();
        assertThat(userRepository.existsByEmail("owner01@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("none@example.com")).isFalse();

        // 본인 이메일은 중복이 아니고, 다른 계정이 쓰는 이메일은 중복이다
        assertThat(userRepository.existsByEmailAndUserIdNot("owner01@example.com", owner.getUserId())).isFalse();
        assertThat(userRepository.existsByEmailAndUserIdNot("owner02@example.com", owner.getUserId())).isTrue();
        assertThat(userRepository.existsByEmailAndUserIdNot("owner02@example.com", other.getUserId())).isFalse();
    }

    @Test
    @DisplayName("역할별 계정 존재 여부")
    void existsByRole() {
        signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);

        assertThat(userRepository.existsByRole(UserRole.STORE_OWNER)).isTrue();
        assertThat(userRepository.existsByRole(UserRole.HQ_ADMIN)).isFalse();

        userRepository.save(User.createHqAdmin("hq_admin01", "hashed", "홍길동", "admin@example.com", "010-0000-0000"));

        assertThat(userRepository.existsByRole(UserRole.HQ_ADMIN)).isTrue();
    }

    @Test
    @DisplayName("같은 로그인 아이디나 이메일은 UNIQUE 제약으로 저장할 수 없다")
    void uniqueConstraints() {
        signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);

        assertThatThrownBy(() -> signUp("store_owner01", "다른사람", "other@example.com", UserRole.STORE_OWNER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 이메일은 UNIQUE 제약으로 저장할 수 없다")
    void uniqueEmail() {
        signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);

        assertThatThrownBy(() -> signUp("store_owner02", "다른사람", "owner01@example.com", UserRole.STORE_OWNER))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("목록 검색: 조건이 모두 null이면 전체를 최신순으로 돌려준다")
    void searchAll() {
        User first = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        User second = signUp("wh_manager01", "이창고", "wh01@example.com", UserRole.WAREHOUSE_MANAGER);

        List<User> found = userRepository.search(new UserSearchCondition(null, null, null));

        assertThat(found).extracting(User::getUserId)
                .containsExactly(second.getUserId(), first.getUserId());
    }

    @Test
    @DisplayName("목록 검색: 역할과 상태로 필터링한다")
    void searchByRoleAndStatus() {
        User owner = signUp("store_owner01", "김점주", "owner01@example.com", UserRole.STORE_OWNER);
        User pendingOwner = signUp("store_owner02", "박점주", "owner02@example.com", UserRole.STORE_OWNER);
        User manager = signUp("wh_manager01", "이창고", "wh01@example.com", UserRole.WAREHOUSE_MANAGER);
        owner.approve();
        userRepository.save(owner);

        assertThat(userRepository.search(new UserSearchCondition(UserRole.STORE_OWNER, null, null)))
                .extracting(User::getUserId)
                .containsExactlyInAnyOrder(owner.getUserId(), pendingOwner.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(null, UserStatus.PENDING, null)))
                .extracting(User::getUserId)
                .containsExactlyInAnyOrder(pendingOwner.getUserId(), manager.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(UserRole.STORE_OWNER, UserStatus.ACTIVE, null)))
                .extracting(User::getUserId)
                .containsExactly(owner.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(UserRole.HQ_ADMIN, null, null))).isEmpty();
    }

    @Test
    @DisplayName("목록 검색: 이름·로그인 아이디·이메일 부분 일치(대소문자 무시), 빈 검색어는 조건 없음")
    void searchByKeyword() {
        User byName = signUp("owner_a", "김점주", "a@example.com", UserRole.STORE_OWNER);
        User byLoginId = signUp("Seoul_Owner", "박점주", "b@example.com", UserRole.STORE_OWNER);
        User byEmail = signUp("owner_c", "최점주", "busan.owner@example.com", UserRole.STORE_OWNER);

        assertThat(userRepository.search(new UserSearchCondition(null, null, "김")))
                .extracting(User::getUserId).containsExactly(byName.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(null, null, "seoul")))
                .extracting(User::getUserId).containsExactly(byLoginId.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(null, null, "BUSAN")))
                .extracting(User::getUserId).containsExactly(byEmail.getUserId());
        assertThat(userRepository.search(new UserSearchCondition(null, null, "  "))).hasSize(3);
        assertThat(userRepository.search(new UserSearchCondition(null, null, "없는검색어"))).isEmpty();
    }
}
