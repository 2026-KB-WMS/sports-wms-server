package com.kb.wms.product.application.port.in;

import java.util.List;

import com.kb.wms.product.application.port.in.command.CategoryOptionGroupAssignCommand;
import com.kb.wms.product.application.port.in.result.CategoryOptionGroupSummary;

/**
 * 카테고리별 옵션 그룹 지정(SPEC·SKU) 유스케이스.
 * PUT, GET /api/v1/products/categories/{categoryId}/option-groups
 */
public interface CategoryOptionGroupUseCase {

    /**
     * 카테고리 자신의 지정을 주어진 목록으로 통째로 바꾸고, 상속분까지 합친 결과를 돌려준다.
     * 없는 카테고리·옵션 그룹은 CATEGORY_NOT_FOUND·OPTION_GROUP_NOT_FOUND, 같은 그룹이 중복되면 VALIDATION_ERROR.
     */
    List<CategoryOptionGroupSummary> assignOptionGroups(CategoryOptionGroupAssignCommand command);

    /** 상위 카테고리에서 상속한 지정을 포함해, 가까운 카테고리의 지정이 우선한 결과. 없으면 CATEGORY_NOT_FOUND. */
    List<CategoryOptionGroupSummary> getOptionGroups(Long categoryId);
}
