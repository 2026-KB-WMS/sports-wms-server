package com.kb.wms.inventory.adapter.in.web.dto.response;

import java.util.List;

/**
 * GET /api/v1/inventory/{inventoryId}/transactions 응답.
 * 페이지네이션은 프로젝트 전체에 아직 도입하지 않아 page_info는 내려주지 않는다(다른 도메인 목록 API와 동일).
 */
public record InventoryTransactionHistoryResponse(
        Long inventoryLotId,
        List<InventoryTransactionResponse> items
) {

    public static InventoryTransactionHistoryResponse of(Long inventoryLotId, List<InventoryTransactionResponse> items) {
        return new InventoryTransactionHistoryResponse(inventoryLotId, items);
    }
}
