package com.kb.wms.inbound.domain.entity;

import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * SKU와 공급처의 다대다 매핑. 본사 관리자가 발주를 확정할 때 라인의 공급처로 고를 수 있는 후보를 정한다.
 * 한 SKU에 공급처를 여러 개 연결할 수 있고, 같은 (SKU, 공급처) 쌍은 한 번만 둘 수 있다.
 * 변경할 속성이 없어 등록과 삭제만 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkuSupplier {

    private Long skuSupplierId;
    private Long skuId;
    private Long supplierId;
    private LocalDateTime createdAt;

    @Builder
    private SkuSupplier(Long skuSupplierId, Long skuId, Long supplierId, LocalDateTime createdAt) {
        this.skuSupplierId = skuSupplierId;
        this.skuId = skuId;
        this.supplierId = supplierId;
        this.createdAt = createdAt;
    }

    public static SkuSupplier register(Long skuId, Long supplierId) {
        if (skuId == null) {
            throw new IllegalArgumentException("SKU ID는 필수입니다.");
        }
        if (supplierId == null) {
            throw new IllegalArgumentException("공급처 ID는 필수입니다.");
        }
        return SkuSupplier.builder()
                .skuId(skuId)
                .supplierId(supplierId)
                .build();
    }
}
