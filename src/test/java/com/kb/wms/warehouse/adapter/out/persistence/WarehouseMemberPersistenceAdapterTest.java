package com.kb.wms.warehouse.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.TestPages;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortOrder;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.warehouse.application.port.in.result.WarehouseMemberView;
import com.kb.wms.warehouse.application.port.out.WarehouseMemberRepository;
import com.kb.wms.warehouse.domain.entity.WarehouseMember;

/**
 * 창고 관리자 배정 목록 조회: 사용자 테이블을 ID로 조인해 이름·로그인 아이디를 붙이고 keyword로 거른다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class WarehouseMemberPersistenceAdapterTest {

    private static final PageCondition WM_PAGE = TestPages.firstPage(TestPages.order("m.assignedAt", SortDirection.DESC), TestPages.order("m.warehouseMemberId", SortDirection.DESC));

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    @Autowired WarehouseMemberRepository warehouseMemberRepository;
    @Autowired UserRepository userRepository;

    private User manager(String loginId, String name) {
        return userRepository.save(User.signUp(loginId, "hashed", name, loginId + "@example.com",
                "010-1234-5678", UserRole.WAREHOUSE_MANAGER));
    }

    private void assign(Long warehouseId, Long userId) {
        warehouseMemberRepository.save(WarehouseMember.assign(warehouseId, userId, "MANAGER", NOW));
    }

    private List<Long> warehouseIds(List<WarehouseMemberView> views) {
        return views.stream().map(WarehouseMemberView::warehouseId).sorted().toList();
    }

    @Test
    @DisplayName("조건이 없으면 모든 배정을 사용자 이름·로그인 아이디와 함께 반환한다")
    void search_all() {
        User kim = manager("kim_wh01", "김창고");
        User park = manager("park_wh02", "박창고");
        assign(1L, kim.getUserId());
        assign(1L, park.getUserId());
        assign(2L, kim.getUserId());

        List<WarehouseMemberView> result = warehouseMemberRepository.search(null, null, null, WM_PAGE).items();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(WarehouseMemberView::userName).containsExactlyInAnyOrder("김창고", "박창고", "김창고");
        assertThat(result).extracting(WarehouseMemberView::loginId)
                .containsExactlyInAnyOrder("kim_wh01", "park_wh02", "kim_wh01");
        assertThat(result).extracting(WarehouseMemberView::memberRole).containsOnly("MANAGER");
    }

    @Test
    @DisplayName("창고·사용자 조건으로 거른다")
    void search_byWarehouseAndUser() {
        User kim = manager("kim_wh01", "김창고");
        User park = manager("park_wh02", "박창고");
        assign(1L, kim.getUserId());
        assign(1L, park.getUserId());
        assign(2L, kim.getUserId());

        assertThat(warehouseIds(warehouseMemberRepository.search(1L, null, null, WM_PAGE).items())).containsExactly(1L, 1L);
        assertThat(warehouseIds(warehouseMemberRepository.search(null, kim.getUserId(), null, WM_PAGE).items())).containsExactly(1L, 2L);
        assertThat(warehouseMemberRepository.search(2L, park.getUserId(), null, WM_PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("keyword는 이름·로그인 아이디를 대소문자 구분 없이 부분 일치로 찾는다")
    void search_byKeyword() {
        User kim = manager("kim_wh01", "김창고");
        User park = manager("park_wh02", "박창고");
        assign(1L, kim.getUserId());
        assign(1L, park.getUserId());

        assertThat(warehouseMemberRepository.search(null, null, "김", WM_PAGE).items())
                .extracting(WarehouseMemberView::userId).containsExactly(kim.getUserId());
        assertThat(warehouseMemberRepository.search(null, null, "PARK_WH", WM_PAGE).items())
                .extracting(WarehouseMemberView::userId).containsExactly(park.getUserId());
        assertThat(warehouseMemberRepository.search(null, null, "없는사람", WM_PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("빈 keyword는 조건 없음으로 본다")
    void search_blankKeywordIsIgnored() {
        User kim = manager("kim_wh01", "김창고");
        assign(1L, kim.getUserId());

        assertThat(warehouseMemberRepository.search(null, null, "   ", WM_PAGE).items()).hasSize(1);
        assertThat(warehouseMemberRepository.search(null, null, "", WM_PAGE).items()).hasSize(1);
    }

    @Test
    @DisplayName("페이지 크기만큼 자르고 필터가 반영된 전체 건수를 돌려준다")
    void search_paging() {
        User kim = manager("kim_wh01", "김창고");
        User park = manager("park_wh02", "박창고");
        assign(1L, kim.getUserId());
        assign(1L, park.getUserId());
        assign(2L, kim.getUserId());
        List<SortOrder> byName = List.of(
                TestPages.order("u.name", SortDirection.ASC), TestPages.order("m.warehouseMemberId", SortDirection.ASC));

        PageResult<WarehouseMemberView> first = warehouseMemberRepository.search(null, null, "_wh0", new PageCondition(1, 2, byName));
        PageResult<WarehouseMemberView> second = warehouseMemberRepository.search(null, null, "_wh0", new PageCondition(2, 2, byName));

        assertThat(warehouseIds(first.items())).containsExactly(1L, 2L);
        assertThat(first.items()).extracting(WarehouseMemberView::userName).containsOnly("김창고");
        assertThat(second.items()).extracting(WarehouseMemberView::userName).containsExactly("박창고");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }
}
