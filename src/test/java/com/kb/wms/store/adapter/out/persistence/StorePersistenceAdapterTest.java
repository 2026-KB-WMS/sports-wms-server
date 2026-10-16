package com.kb.wms.store.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.paging.SortDirection;
import com.kb.wms.common.paging.SortOrder;
import com.kb.wms.common.paging.TestPages;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;

/**
 * 지점 목록 검색: 페이지 자르기, 필터가 반영된 전체 건수, 정렬.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class StorePersistenceAdapterTest {

    @Autowired StoreRepository storeRepository;

    private Store store(String code, String name) {
        return storeRepository.save(Store.register(code, name, "서울 강남구", "김점주", "02-333-1234"));
    }

    @Test
    @DisplayName("페이지 크기만큼 자르고 필터가 반영된 전체 건수와 정렬을 돌려준다")
    void search_paging() {
        store("PG-C", "다페이징점");
        store("PG-A", "가페이징점");
        store("PG-B", "나페이징점");
        store("OTHER", "라다른점");
        StoreSearchCondition condition = new StoreSearchCondition("페이징", null);
        List<SortOrder> byName = List.of(
                TestPages.order("s.name", SortDirection.ASC), TestPages.order("s.storeId", SortDirection.ASC));

        PageResult<Store> first = storeRepository.search(condition, new PageCondition(1, 2, byName));
        PageResult<Store> second = storeRepository.search(condition, new PageCondition(2, 2, byName));

        assertThat(first.items()).extracting(Store::getName).containsExactly("가페이징점", "나페이징점");
        assertThat(second.items()).extracting(Store::getName).containsExactly("다페이징점");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("활성 여부 필터는 건수에도 반영된다")
    void search_countReflectsActiveFilter() {
        Store inactive = store("PGI-A", "비활성페이징점");
        inactive.deactivate();
        storeRepository.save(inactive);
        store("PGI-B", "활성페이징점");
        List<SortOrder> byId = List.of(TestPages.order("s.storeId", SortDirection.ASC));

        PageResult<Store> active = storeRepository.search(
                new StoreSearchCondition("페이징", true), new PageCondition(1, 10, byId));
        PageResult<Store> inactiveOnly = storeRepository.search(
                new StoreSearchCondition("페이징", false), new PageCondition(1, 10, byId));

        assertThat(active.items()).extracting(Store::getName).containsExactly("활성페이징점");
        assertThat(active.totalElements()).isEqualTo(1);
        assertThat(inactiveOnly.items()).extracting(Store::getName).containsExactly("비활성페이징점");
        assertThat(inactiveOnly.totalElements()).isEqualTo(1);
    }
}
