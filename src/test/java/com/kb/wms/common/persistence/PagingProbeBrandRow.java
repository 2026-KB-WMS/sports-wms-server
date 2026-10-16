package com.kb.wms.common.persistence;

/** {@link PagingProbeRepository#searchBrands}의 조회 결과 행 (집계 값 포함) */
public record PagingProbeBrandRow(Long brandId, String brandName, Long productCount, Long idSum) {
}
