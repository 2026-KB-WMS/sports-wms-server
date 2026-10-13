package com.kb.wms.product.application.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.in.ProductCodeUseCase;
import com.kb.wms.product.application.port.in.result.CodeItem;
import com.kb.wms.product.domain.enums.SkuUnit;

/**
 * 상품 도메인 고정 코드 목록 조회. DB 조회 없이 확정된 enum 값을 그대로 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class ProductCodeService implements ProductCodeUseCase {

    @Override
    public List<CodeItem> getSkuUnits() {
        return Arrays.stream(SkuUnit.values())
                .map(unit -> new CodeItem(unit.getCode(), unit.getDescription()))
                .toList();
    }
}
