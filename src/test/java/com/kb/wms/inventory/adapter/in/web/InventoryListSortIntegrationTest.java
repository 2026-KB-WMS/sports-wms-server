package com.kb.wms.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.SortSpec;
import com.kb.wms.common.security.AuthenticatedUser;
import com.kb.wms.common.security.UserRole;
import com.kb.wms.inventory.application.port.in.InventoryQueryUseCase;
import com.kb.wms.inventory.application.port.in.LotUseCase;
import com.kb.wms.inventory.application.port.in.query.InventoryLotSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventorySectionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.InventoryTransactionSearchCondition;
import com.kb.wms.inventory.application.port.in.query.LotSearchCondition;

/**
 * 재고 목록 5개가 컨트롤러에 정의된 정렬 화이트리스트의 모든 필드·방향으로 실제 쿼리(집계식·case·coalesce 포함)와
 * 건수 쿼리까지 오류 없이 도는지 확인한다. 2페이지를 요청해 건수 쿼리를 반드시 실행하게 한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:inventory_list_sort;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class InventoryListSortIntegrationTest {

    private static final AuthenticatedUser HQ = new AuthenticatedUser(1L, UserRole.HQ_ADMIN, List.of(), List.of());

    @Autowired private InventoryQueryUseCase inventoryQueryUseCase;
    @Autowired private LotUseCase lotUseCase;

    @Test
    @DisplayName("SKU별 재고 목록은 허용된 모든 정렬 필드로 조회된다")
    void inventories_allSortFields() {
        forEachSort(InventoryController.SKU_SORT, page -> inventoryQueryUseCase.getInventories(
                InventorySearchCondition.unscoped(null, null, null), page, HQ));
    }

    @Test
    @DisplayName("구역별 재고 목록은 허용된 모든 정렬 필드로 조회된다")
    void inventoriesBySection_allSortFields() {
        forEachSort(InventoryController.SECTION_SORT, page -> inventoryQueryUseCase.getInventoriesBySection(
                InventorySectionSearchCondition.unscoped(null, null, null, null, null), page, HQ));
    }

    @Test
    @DisplayName("로트별 재고 목록은 허용된 모든 정렬 필드로 조회된다")
    void inventoriesByLot_allSortFields() {
        forEachSort(InventoryController.LOT_VIEW_SORT, page -> inventoryQueryUseCase.searchInventoriesByLot(
                InventoryLotSearchCondition.unscoped(null, null, null, null, null, null, null), page, HQ));
    }

    @Test
    @DisplayName("재고 이력 목록은 허용된 모든 정렬 필드로 조회된다")
    void transactions_allSortFields() {
        forEachSort(InventoryController.TRANSACTION_SORT, page -> inventoryQueryUseCase.getTransactions(
                InventoryTransactionSearchCondition.unscoped(null, null, null, null, null, null, null, null, null, null),
                page, HQ));
    }

    @Test
    @DisplayName("로트 목록은 허용된 모든 정렬 필드로 조회된다")
    void lots_allSortFields() {
        forEachSort(LotController.LIST_SORT, page -> lotUseCase.getLots(
                LotSearchCondition.unscoped(null, null, null, null), page, HQ));
    }

    private static void forEachSort(SortSpec spec, java.util.function.Consumer<PageCondition> query) {
        List<String> failures = new ArrayList<>();
        for (String field : spec.fieldNames()) {
            for (String direction : List.of("asc", "desc")) {
                run(failures, field + "," + direction, () -> query.accept(PageCondition.of(2, 1, field + "," + direction, spec)));
            }
        }
        // 정렬을 생략한 기본 정렬도 돈다
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
