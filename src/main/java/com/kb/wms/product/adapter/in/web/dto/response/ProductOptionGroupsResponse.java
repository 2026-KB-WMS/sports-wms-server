package com.kb.wms.product.adapter.in.web.dto.response;

import java.util.List;

import com.kb.wms.product.application.port.in.result.OptionValueSummary;
import com.kb.wms.product.application.port.in.result.ProductOptionGroupSummary;

/**
 * GET /api/v1/products/{productId}/option-groups 응답.
 * 옵션 그룹 수가 적어 공통 페이지네이션(page_info)을 적용하지 않는다.
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
            List<Value> values
    ) {

        public static Item from(ProductOptionGroupSummary summary) {
            List<Value> values = summary.values().stream()
                    .map(Value::from)
                    .toList();
            return new Item(summary.optionGroupId(), summary.name(), values);
        }
    }

    public record Value(
            Long optionValueId,
            String value,
            int sortOrder
    ) {

        public static Value from(OptionValueSummary summary) {
            return new Value(summary.optionValueId(), summary.value(), summary.sortOrder());
        }
    }
}
