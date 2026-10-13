package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.ProductOptionGroupSummary;

/**
 * GET /api/v1/products/{productId}/option-groups 응답.
 * 옵션 그룹 수가 적어 공통 페이지네이션(page_info)을 적용하지 않는다.
 * Item은 GET /api/v1/products/option-groups 응답 항목으로도 쓴다.
 */
public record ProductOptionGroupsResponse(
        Long productId,
        List<Item> items
) {

    public static ProductOptionGroupsResponse of(Long productId, List<ProductOptionGroupSummary> groups) {
        List<Item> items = groups.stream()
                .map(Item::from)
                .toList();
        return new ProductOptionGroupsResponse(productId, items);
    }

    public record Item(
            Long optionGroupId,
            String name,
            List<OptionValueItemResponse> values
    ) {

        public static Item from(ProductOptionGroupSummary summary) {
            List<OptionValueItemResponse> values = summary.values().stream()
                    .map(OptionValueItemResponse::from)
                    .toList();
            return new Item(summary.optionGroupId(), summary.name(), values);
        }
    }
}
