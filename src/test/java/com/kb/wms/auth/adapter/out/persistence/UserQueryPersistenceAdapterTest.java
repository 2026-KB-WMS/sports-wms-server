package com.kb.wms.auth.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.auth.domain.enums.UserRole;
import com.kb.wms.store.adapter.out.persistence.entity.StoreMemberJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreMemberJpaRepository;
import com.kb.wms.warehouse.adapter.out.persistence.entity.WarehouseMemberJpaEntity;
import com.kb.wms.warehouse.adapter.out.persistence.repository.WarehouseMemberJpaRepository;

/**
 * 회원 읽기 전용 조회(소속 ID, 이름) 검증. 소속은 다른 도메인 테이블을 ID로만 읽는다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class UserQueryPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    @Autowired UserQueryRepository userQueryRepository;
    @Autowired UserRepository userRepository;
    @Autowired WarehouseMemberJpaRepository warehouseMemberJpaRepository;
    @Autowired StoreMemberJpaRepository storeMemberJpaRepository;

    private User user(String loginId, String name, UserRole role) {
        return userRepository.save(User.signUp(loginId, "hashed", name, loginId + "@example.com", "010-1234-5678", role));
    }

    private void assignWarehouse(Long warehouseId, Long userId) {
        warehouseMemberJpaRepository.save(WarehouseMemberJpaEntity.builder()
                .warehouseId(warehouseId).userId(userId).memberRole("MANAGER").assignedAt(NOW).build());
    }

    private void assignStore(Long storeId, Long userId) {
        storeMemberJpaRepository.save(StoreMemberJpaEntity.builder()
                .storeId(storeId).userId(userId).memberRole("OWNER").assignedAt(NOW).build());
    }

    @Test
    @DisplayName("여러 창고에 배정된 사용자의 창고 ID를 오름차순으로 돌려준다")
    void warehouseAffiliation() {
        User manager = user("wh_manager01", "이창고", UserRole.WAREHOUSE_MANAGER);
        User other = user("wh_manager02", "최창고", UserRole.WAREHOUSE_MANAGER);
        assignWarehouse(7L, manager.getUserId());
        assignWarehouse(3L, manager.getUserId());
        assignWarehouse(5L, other.getUserId());

        UserAffiliation affiliation = userQueryRepository.findAffiliation(manager.getUserId());

        assertThat(affiliation.warehouseIds()).containsExactly(3L, 7L);
        assertThat(affiliation.storeIds()).isEmpty();
        assertThat(affiliation.hasAny()).isTrue();
    }

    @Test
    @DisplayName("점주의 지점 ID를 돌려준다")
    void storeAffiliation() {
        User owner = user("store_owner01", "김점주", UserRole.STORE_OWNER);
        User other = user("store_owner02", "박점주", UserRole.STORE_OWNER);
        assignStore(2L, owner.getUserId());
        assignStore(9L, other.getUserId());

        UserAffiliation affiliation = userQueryRepository.findAffiliation(owner.getUserId());

        assertThat(affiliation.storeIds()).containsExactly(2L);
        assertThat(affiliation.warehouseIds()).isEmpty();
        assertThat(affiliation.hasAny()).isTrue();
    }

    @Test
    @DisplayName("소속이 없으면 빈 소속을 돌려준다")
    void noAffiliation() {
        User owner = user("store_owner01", "김점주", UserRole.STORE_OWNER);

        UserAffiliation affiliation = userQueryRepository.findAffiliation(owner.getUserId());

        assertThat(affiliation.warehouseIds()).isEmpty();
        assertThat(affiliation.storeIds()).isEmpty();
        assertThat(affiliation.hasAny()).isFalse();
    }

    @Test
    @DisplayName("사용자 ID별 이름을 돌려주고 없는 ID는 제외한다")
    void findNamesByIds() {
        User owner = user("store_owner01", "김점주", UserRole.STORE_OWNER);
        User manager = user("wh_manager01", "이창고", UserRole.WAREHOUSE_MANAGER);
        user("store_owner02", "박점주", UserRole.STORE_OWNER);

        Map<Long, String> names = userQueryRepository.findNamesByIds(
                Set.of(owner.getUserId(), manager.getUserId(), 999_999L));

        assertThat(names).containsOnly(
                Map.entry(owner.getUserId(), "김점주"),
                Map.entry(manager.getUserId(), "이창고"));
    }

    @Test
    @DisplayName("빈 ID 목록은 빈 맵")
    void findNamesByEmptyIds() {
        assertThat(userQueryRepository.findNamesByIds(List.of())).isEmpty();
    }
}
