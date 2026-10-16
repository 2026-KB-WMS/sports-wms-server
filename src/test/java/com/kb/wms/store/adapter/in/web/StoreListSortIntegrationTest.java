package com.kb.wms.store.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.store.application.port.in.StoreMemberUseCase;
import com.kb.wms.store.application.port.in.StoreUseCase;
import com.kb.wms.store.application.port.in.query.StoreSearchCondition;

/**
 * 지점·지점 담당자 목록이 컨트롤러에 정의된 정렬 화이트리스트의 모든 필드·방향으로 실제 쿼리와 건수 쿼리까지
 * 오류 없이 도는지 확인한다. 2페이지를 요청해 건수 쿼리를 반드시 실행하게 한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:store_list_sort;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class StoreListSortIntegrationTest {

    @Autowired private StoreUseCase storeUseCase;
    @Autowired private StoreMemberUseCase storeMemberUseCase;

    @Test
    @DisplayName("지점 목록은 허용된 모든 정렬 필드로 조회된다")
    void stores_allSortFields() {
        forEachSort(StoreController.LIST_SORT, page -> storeUseCase.getStores(
                new StoreSearchCondition(null, null), page));
    }

    @Test
    @DisplayName("지점 담당자 목록은 허용된 모든 정렬 필드로 조회된다")
    void storeManagers_allSortFields() {
        forEachSort(StoreMemberController.LIST_SORT, page -> storeMemberUseCase.getManagers(null, null, null, page));
    }

    private static void forEachSort(SortSpec spec, Consumer<PageCondition> query) {
        List<String> failures = new ArrayList<>();
        for (String field : spec.fieldNames()) {
            for (String direction : List.of("asc", "desc")) {
                run(failures, field + "," + direction,
                        () -> query.accept(PageCondition.of(2, 1, field + "," + direction, spec)));
            }
        }
        run(failures, "(기본 정렬)", () -> query.accept(PageCondition.of(2, 1, null, spec)));
        assertThat(failures).as("쿼리가 실패한 정렬").isEmpty();
    }

    private static void run(List<String> failures, String label, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            String message = String.valueOf(e.getMessage());
            failures.add(label + " -> " + message.substring(0, Math.min(message.length(), 300)));
        }
    }
}
