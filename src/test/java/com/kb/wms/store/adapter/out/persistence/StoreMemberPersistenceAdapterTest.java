package com.kb.wms.store.adapter.out.persistence;

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
import com.kb.wms.store.application.port.in.result.StoreMemberView;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.domain.entity.StoreMember;

/**
 * 지점 관리자 배정 목록 조회: 사용자 테이블을 ID로 조인해 이름·로그인 아이디를 붙이고 keyword로 거른다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StoreMemberPersistenceAdapterTest {

    private static final PageCondition SM_PAGE = TestPages.firstPage(TestPages.order("m.assignedAt", SortDirection.DESC), TestPages.order("m.storeMemberId", SortDirection.DESC));

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 9, 0);

    @Autowired StoreMemberRepository storeMemberRepository;
    @Autowired UserRepository userRepository;

    private User owner(String loginId, String name) {
        return userRepository.save(User.signUp(loginId, "hashed", name, loginId + "@example.com",
                "010-1234-5678", UserRole.STORE_OWNER));
    }

    private void assign(Long storeId, Long userId, LocalDateTime assignedAt) {
        storeMemberRepository.save(StoreMember.assign(storeId, userId, "OWNER", assignedAt));
    }

    private List<Long> storeIds(List<StoreMemberView> views) {
        return views.stream().map(StoreMemberView::storeId).toList();
    }

    @Test
    @DisplayName("조건이 없으면 모든 배정을 사용자 이름·로그인 아이디와 함께 배정 일시 내림차순으로 반환한다")
    void search_all() {
        User kim = owner("kim_owner01", "김점주");
        User park = owner("park_owner02", "박점주");
        assign(1L, kim.getUserId(), NOW);
        assign(1L, park.getUserId(), NOW.plusHours(1));
        assign(2L, kim.getUserId(), NOW.plusHours(2));

        List<StoreMemberView> result = storeMemberRepository.search(null, null, null, SM_PAGE).items();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(StoreMemberView::assignedAt)
                .containsExactly(NOW.plusHours(2), NOW.plusHours(1), NOW);
        assertThat(result.get(1).userName()).isEqualTo("박점주");
        assertThat(result.get(1).loginId()).isEqualTo("park_owner02");
        assertThat(result.get(1).memberRole()).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("지점·사용자 조건으로 거른다")
    void search_byStoreAndUser() {
        User kim = owner("kim_owner01", "김점주");
        User park = owner("park_owner02", "박점주");
        assign(1L, kim.getUserId(), NOW);
        assign(1L, park.getUserId(), NOW.plusHours(1));
        assign(2L, kim.getUserId(), NOW.plusHours(2));

        assertThat(storeIds(storeMemberRepository.search(1L, null, null, SM_PAGE).items())).containsExactly(1L, 1L);
        assertThat(storeIds(storeMemberRepository.search(null, kim.getUserId(), null, SM_PAGE).items())).containsExactly(2L, 1L);
        assertThat(storeMemberRepository.search(2L, park.getUserId(), null, SM_PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("keyword는 이름·로그인 아이디를 대소문자 구분 없이 부분 일치로 찾는다")
    void search_byKeyword() {
        User kim = owner("kim_owner01", "김점주");
        User park = owner("park_owner02", "박점주");
        assign(1L, kim.getUserId(), NOW);
        assign(1L, park.getUserId(), NOW.plusHours(1));

        assertThat(storeMemberRepository.search(null, null, "김", SM_PAGE).items())
                .extracting(StoreMemberView::userId).containsExactly(kim.getUserId());
        assertThat(storeMemberRepository.search(null, null, "PARK_OWNER", SM_PAGE).items())
                .extracting(StoreMemberView::userId).containsExactly(park.getUserId());
        assertThat(storeMemberRepository.search(null, null, "없는사람", SM_PAGE).items()).isEmpty();
    }

    @Test
    @DisplayName("빈 keyword는 조건 없음으로 본다")
    void search_blankKeywordIsIgnored() {
        User kim = owner("kim_owner01", "김점주");
        assign(1L, kim.getUserId(), NOW);

        assertThat(storeMemberRepository.search(null, null, "   ", SM_PAGE).items()).hasSize(1);
        assertThat(storeMemberRepository.search(null, null, "", SM_PAGE).items()).hasSize(1);
    }

    @Test
    @DisplayName("페이지 크기만큼 자르고 필터가 반영된 전체 건수를 돌려준다")
    void search_paging() {
        User kim = owner("kim_owner01", "김점주");
        User park = owner("park_owner02", "박점주");
        assign(1L, kim.getUserId(), NOW);
        assign(1L, park.getUserId(), NOW.plusHours(1));
        assign(2L, kim.getUserId(), NOW.plusHours(2));
        List<SortOrder> byName = List.of(
                TestPages.order("u.name", SortDirection.ASC), TestPages.order("m.storeMemberId", SortDirection.ASC));

        PageResult<StoreMemberView> first = storeMemberRepository.search(null, null, "_owner0", new PageCondition(1, 2, byName));
        PageResult<StoreMemberView> second = storeMemberRepository.search(null, null, "_owner0", new PageCondition(2, 2, byName));

        assertThat(storeIds(first.items())).containsExactly(1L, 2L);
        assertThat(first.items()).extracting(StoreMemberView::userName).containsOnly("김점주");
        assertThat(second.items()).extracting(StoreMemberView::userName).containsExactly("박점주");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }
}
