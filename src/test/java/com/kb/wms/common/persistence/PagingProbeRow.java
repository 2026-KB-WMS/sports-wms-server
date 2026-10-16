package com.kb.wms.common.persistence;

/** {@link PagingProbeRepository}의 조회 결과 행 */
public record PagingProbeRow(Long productId, String productName, String brandName) {
}
